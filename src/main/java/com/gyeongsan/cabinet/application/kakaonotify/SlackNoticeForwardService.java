package com.gyeongsan.cabinet.application.kakaonotify;

import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelException;
import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelMessage;
import com.gyeongsan.cabinet.domain.alarm.model.SlackHistory;
import com.gyeongsan.cabinet.domain.alarm.port.out.SlackChannelPort;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInsufficientScopeException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInvalidGrantException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoMessage;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotificationException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoRecipient;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoRefreshedToken;
import com.gyeongsan.cabinet.domain.kakaonotify.model.TokenDecryptionException;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.ForwardSlackNoticesUseCase;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoConsentRepositoryPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoNotificationPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.NoticeCursorPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.TokenCipherPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 슬랙 공지 채널의 새 글을 카카오 알림 수신 대상(동의 유효 AND 알림 켜짐)에게 "나에게 보내기"로 전달한다. 구조는 오류제보 포워더({@code
 * SlackReportForwardService})와 같다.
 *
 * <ul>
 *   <li>중복 방지: 채널별 마지막 확인 ts 커서. 커서가 없으면 과거 글은 보내지 않고 가장 최근 글에서 시작한다. 커서를 못 읽으면 그 주기는 건너뛴다.
 *   <li>수신자가 없거나 조회에 실패하면 경고만 남기고 <b>커서를 옮기지 않아</b> 공지가 조용히 사라지지 않게 한다. 다만 {@code maxHoldHours} 보다
 *       오래된 공지는 새 수신자에게 쏟아내지 않도록 경고와 함께 건너뛴다.
 *   <li>개별 발송 실패는 로그만 남기고 다음 사람으로 넘어간다. 카카오가 동의 해지({@code invalid_grant}, 발송 시 -402)로 거절하면 그 동의를 해지
 *       처리해 이후 대상에서 뺀다.
 *   <li>각 수신자는 발송 직전에 동의/알림 스위치를 다시 확인하고, 한 번의 실행에서는 refresh_token 갱신을 사람당 한 번만 한다.
 * </ul>
 */
@Log4j2
@RequiredArgsConstructor
public class SlackNoticeForwardService implements ForwardSlackNoticesUseCase {

    private static final String PREFIX = "📢 [공지] ";

    private final SlackChannelPort channelPort;
    private final NoticeCursorPort cursorPort;
    private final KakaoConsentRepositoryPort consentRepository;
    private final KakaoNotificationPort kakao;
    private final TokenCipherPort cipher;
    private final SlackNoticeSettings settings;
    private final Clock clock;

    /** 한 번의 실행 동안 한 수신자의 상태. */
    private static final class Session {
        String accessToken;
        boolean unavailable;
        String currentEncryptedToken;
    }

    @Override
    public void forwardNewNotices() {
        String channelId = settings.channelId();

        Optional<String> cursor;
        try {
            cursor = cursorPort.getCursor(channelId);
        } catch (RuntimeException e) {
            log.warn("[SlackNotice] 커서를 읽지 못해 이번 주기를 건너뜁니다: {}", e.toString());
            return;
        }
        if (cursor.isEmpty()) {
            startFromNow(channelId);
            return;
        }

        SlackHistory history;
        try {
            history = channelPort.fetchNewerThan(channelId, cursor.get());
        } catch (SlackChannelException e) {
            log.warn("[SlackNotice] 채널 조회 실패, 다음 주기에 다시 시도합니다: {}", e.getMessage());
            return;
        }

        List<SlackChannelMessage> all = history.messagesOldestFirst();
        if (all.isEmpty()) {
            return;
        }
        List<SlackChannelMessage> forwardable =
                all.stream().filter(m -> !m.isSystemMessage()).toList();

        // 오래 보류된 공지는 새 수신자에게 쏟아내지 않는다(조용히 버리지 않고 경고를 남긴다).
        List<SlackChannelMessage> fresh = forwardable.stream().filter(m -> !isTooOld(m)).toList();
        int expired = forwardable.size() - fresh.size();
        if (expired > 0) {
            log.warn(
                    "[SlackNotice] 보류 한도({}시간)를 넘긴 공지 {}건은 전달하지 않고 건너뜁니다.",
                    settings.maxHoldHours(),
                    expired);
        }
        if (fresh.isEmpty()) {
            saveCursor(channelId, all.get(all.size() - 1).ts());
            return;
        }

        List<KakaoRecipient> recipients = loadRecipients();
        if (recipients.isEmpty()) {
            return;
        }

        int skipped = Math.max(0, fresh.size() - settings.maxPerPoll());
        List<SlackChannelMessage> toForward = fresh.subList(skipped, fresh.size());

        Map<Long, Session> sessions = new HashMap<>();
        int sent = 0;
        if (skipped > 0 || (history.truncated() && !fresh.isEmpty())) {
            sent += sendToAll(recipients, sessions, summaryMessage(skipped, history.truncated()));
        }
        for (SlackChannelMessage message : toForward) {
            sent += sendToAll(recipients, sessions, noticeMessage(channelId, message));
            saveCursor(channelId, message.ts());
        }
        saveCursor(channelId, all.get(all.size() - 1).ts());
        log.info(
                "[SlackNotice] 새 글 {}건 확인, {}건 전달, 수신자 {}명, 발송 {}건 (생략 {}건)",
                all.size(),
                toForward.size(),
                recipients.size(),
                sent,
                skipped);
    }

    private void startFromNow(String channelId) {
        String start;
        try {
            start = channelPort.latestTs(channelId).orElseGet(this::nowTs);
        } catch (SlackChannelException e) {
            log.warn("[SlackNotice] 시작 지점을 정하지 못했습니다. 다음 주기에 다시 시도합니다: {}", e.getMessage());
            return;
        }
        saveCursor(channelId, start);
        log.info("[SlackNotice] 커서가 없어 과거 공지는 보내지 않고 지금({})부터 시작합니다.", start);
    }

    private String nowTs() {
        Instant now = clock.instant();
        return String.format("%d.%06d", now.getEpochSecond(), now.getNano() / 1000);
    }

    private boolean isTooOld(SlackChannelMessage message) {
        try {
            long seconds = Long.parseLong(message.ts().split("\\.")[0]);
            long ageSeconds = clock.instant().getEpochSecond() - seconds;
            return ageSeconds > settings.maxHoldHours() * 3600L;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void saveCursor(String channelId, String ts) {
        try {
            cursorPort.saveCursor(channelId, ts);
        } catch (RuntimeException e) {
            log.error("[SlackNotice] 커서 저장 실패(다음 주기에 같은 공지가 다시 전달될 수 있음): {}", e.toString());
        }
    }

    /** 수신자를 못 구하면 빈 목록(이번 주기는 보류). 부팅이나 스케줄러를 막지 않고 경고만 남긴다. */
    private List<KakaoRecipient> loadRecipients() {
        try {
            List<KakaoRecipient> recipients = consentRepository.findActiveRecipients();
            if (recipients.isEmpty()) {
                log.warn(
                        "[SlackNotice] 알림 수신 대상(동의 유효 + 알림 켜짐)이 없어 전달을 보류합니다. 대상이 생기면 다음 주기에 전달됩니다.");
            }
            return recipients;
        } catch (RuntimeException e) {
            log.warn("[SlackNotice] 수신 대상을 조회하지 못해 이번 주기를 건너뜁니다: {}", e.toString());
            return List.of();
        }
    }

    private int sendToAll(
            List<KakaoRecipient> recipients, Map<Long, Session> sessions, KakaoMessage message) {
        int sent = 0;
        for (KakaoRecipient recipient : recipients) {
            Session session = sessions.computeIfAbsent(recipient.userId(), id -> new Session());
            if (session.unavailable) {
                continue;
            }
            try {
                if (session.accessToken == null && !openSession(recipient, session)) {
                    continue;
                }
                kakao.sendToMe(session.accessToken, message);
                sent++;
            } catch (KakaoInsufficientScopeException e) {
                revoke(recipient.userId(), session.currentEncryptedToken, "발송 시 동의 항목 부족(-402)");
                session.unavailable = true;
            } catch (RuntimeException e) {
                // 한 사람의 실패가 다른 사람을 막지 않게 한다. 토큰/본문은 로그에 남기지 않는다.
                log.error(
                        "[SlackNotice] 발송 실패 - userId: {}, 원인: {}",
                        recipient.userId(),
                        e.getClass().getSimpleName());
                session.unavailable = true;
            }
        }
        return sent;
    }

    /**
     * 발송 직전 재확인 → 복호화 → access_token 갱신. 이 사람에게 보낼 수 없으면 false.
     *
     * <p>복호화 실패(키 분실·변경)는 동의를 해지하지 않는다. 키를 잘못 넣은 일시적 실수가 모든 동의를 영구히 지우지 않게 하기 위해서이며, 유저가 다시 동의하면 새
     * 키로 덮어써진다.
     */
    private boolean openSession(KakaoRecipient listed, Session session) {
        Optional<KakaoRecipient> current;
        try {
            current = consentRepository.findActiveRecipient(listed.userId());
        } catch (RuntimeException e) {
            log.warn("[SlackNotice] 수신 상태를 확인하지 못해 건너뜁니다 - userId: {}", listed.userId());
            session.unavailable = true;
            return false;
        }
        if (current.isEmpty()) {
            // 목록을 읽은 뒤 동의가 해지됐거나 알림을 껐다.
            session.unavailable = true;
            return false;
        }

        String encrypted = current.get().encryptedRefreshToken();
        session.currentEncryptedToken = encrypted;
        String context = context(listed.userId());

        String refreshToken;
        try {
            refreshToken = cipher.decrypt(encrypted, context);
        } catch (TokenDecryptionException e) {
            log.error(
                    "[SlackNotice] 토큰을 복호화하지 못해 건너뜁니다(암호화 키가 바뀌었거나 값이 손상됨, 유저가 다시 동의해야 함) - userId: {}",
                    listed.userId());
            session.unavailable = true;
            return false;
        }

        KakaoRefreshedToken refreshed;
        try {
            refreshed = kakao.refreshAccessToken(refreshToken);
        } catch (KakaoInvalidGrantException e) {
            revoke(listed.userId(), encrypted, "refresh_token invalid_grant(유저가 동의를 해지했거나 토큰 만료)");
            session.unavailable = true;
            return false;
        } catch (KakaoNotificationException e) {
            log.warn("[SlackNotice] 토큰 갱신 실패, 이번 실행에서는 건너뜁니다 - userId: {}", listed.userId());
            session.unavailable = true;
            return false;
        }

        if (refreshed.newRefreshToken() != null
                && !refreshed.newRefreshToken().equals(refreshToken)) {
            rotate(listed.userId(), encrypted, refreshed.newRefreshToken(), session);
        }
        session.accessToken = refreshed.accessToken();
        return true;
    }

    private void rotate(Long userId, String oldEncrypted, String newRefreshToken, Session session) {
        try {
            String newEncrypted = cipher.encrypt(newRefreshToken, context(userId));
            if (consentRepository.rotateIfTokenUnchanged(userId, oldEncrypted, newEncrypted)) {
                session.currentEncryptedToken = newEncrypted;
            } else {
                log.warn(
                        "[SlackNotice] refresh_token 회전을 저장하지 못했습니다(그 사이 값이 바뀜) - userId: {}",
                        userId);
            }
        } catch (RuntimeException e) {
            // 저장에 실패해도 이번 발송은 계속한다. 이전 토큰이 아직 유효하면 다음 실행에서 다시 회전된다.
            log.error(
                    "[SlackNotice] 새 refresh_token 저장 실패 - userId: {}, 원인: {}",
                    userId,
                    e.getClass().getSimpleName());
        }
    }

    private void revoke(Long userId, String usedEncryptedToken, String reason) {
        try {
            boolean revoked =
                    consentRepository.revokeIfTokenUnchanged(
                            userId, usedEncryptedToken, LocalDateTime.now(clock));
            log.info(
                    "[SlackNotice] 카카오 동의를 해지 처리했습니다 - userId: {}, 사유: {}{}",
                    userId,
                    reason,
                    revoked ? "" : " (그 사이 값이 바뀌어 변경하지 않음)");
        } catch (RuntimeException e) {
            log.error(
                    "[SlackNotice] 해지 처리 저장 실패 - userId: {}, 원인: {}",
                    userId,
                    e.getClass().getSimpleName());
        }
    }

    static String context(Long userId) {
        return "kakao-notify:" + userId;
    }

    private KakaoMessage summaryMessage(int skipped, boolean truncated) {
        String count = truncated ? skipped + "건 이상" : skipped + "건";
        return new KakaoMessage(
                SlackMrkdwn.truncate(
                        PREFIX + "새 공지가 많아 이전 " + count + "은 생략했습니다. 슬랙에서 확인해 주세요.",
                        KakaoMessage.MAX_TEXT_LENGTH),
                settings.linkUrl());
    }

    private KakaoMessage noticeMessage(String channelId, SlackChannelMessage message) {
        String body = SlackMrkdwn.toPlainText(message.text());
        if (body.isBlank()) {
            body =
                    "(본문 없음"
                            + (message.fileCount() > 0 ? ", 첨부 " + message.fileCount() + "개" : "")
                            + ")";
        } else if (message.fileCount() > 0) {
            body += "\n📎 첨부 " + message.fileCount() + "개";
        }
        return new KakaoMessage(
                SlackMrkdwn.truncate(PREFIX + body, KakaoMessage.MAX_TEXT_LENGTH),
                linkFor(channelId, message));
    }

    /**
     * 카톡 메시지를 눌렀을 때 열릴 주소. 잘린 본문의 전체를 볼 수 있도록 해당 슬랙 글의 퍼머링크를 쓰고, 얻지 못하면(네트워크 오류, 권한 부족, 이상한 값) 기본
     * 주소로 폴백한다. 어떤 경우에도 발송을 막지 않는다. 공지 한 건당 한 번만 조회한다(수신자 수와 무관).
     */
    private String linkFor(String channelId, SlackChannelMessage message) {
        if (!settings.permalinkEnabled()) {
            return settings.linkUrl();
        }
        try {
            Optional<String> link = channelPort.permalink(channelId, message.ts());
            if (link.isPresent() && isHttpUrl(link.get())) {
                return link.get();
            }
            log.warn("[SlackNotice] 슬랙 퍼머링크를 얻지 못해 기본 링크를 씁니다 - ts: {}", message.ts());
        } catch (RuntimeException e) {
            log.warn(
                    "[SlackNotice] 슬랙 퍼머링크 조회 중 오류로 기본 링크를 씁니다 - ts: {}, 원인: {}",
                    message.ts(),
                    e.getClass().getSimpleName());
        }
        return settings.linkUrl();
    }

    private static boolean isHttpUrl(String value) {
        return value.startsWith("https://") || value.startsWith("http://");
    }
}
