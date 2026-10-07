package com.gyeongsan.cabinet.application.alarm;

import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelException;
import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelMessage;
import com.gyeongsan.cabinet.domain.alarm.model.SlackHistory;
import com.gyeongsan.cabinet.domain.alarm.port.in.ForwardSlackReportsUseCase;
import com.gyeongsan.cabinet.domain.alarm.port.out.AlarmPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.ReportCursorPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.ReportRecipientPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.SlackChannelPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 사물함 오류제보 채널의 새 글을 관리자(ADMIN, MASTER 권한 유저 전원)에게 DM 으로 전달한다. 내용은 거르지 않고, 시스템 메시지(입퇴장 등)만 제외한다.
 *
 * <p>수신자는 전달할 글이 있을 때만 DB 에서 읽는다(조용한 주기에는 DB 를 건드리지 않는다). 수신자가 없거나 조회에 실패하면 경고만 남기고 커서를 옮기지 않아, 다음
 * 주기에 같은 글을 다시 시도한다(관리자가 생기면 그때 전달되고, 글이 조용히 사라지지 않는다).
 *
 * <p>중복 전달은 "마지막으로 확인한 메시지 ts" 커서로 막는다. 커서가 없으면(처음 켠 경우) 과거 글을 전달하지 않고 채널의 가장 최근 글에서 시작한다. 전달이 실패해도
 * 오류는 로그로만 남기고(AlarmPort 는 성공 여부를 알려주지 않는다) 다음 글로 넘어간다.
 */
@Log4j2
@RequiredArgsConstructor
public class SlackReportForwardService implements ForwardSlackReportsUseCase {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Seoul"));
    private static final String MASS_MENTION = "<!(channel|here|everyone)(\\|[^>]*)?>";

    private final SlackChannelPort channelPort;
    private final ReportCursorPort cursorPort;
    private final AlarmPort alarmPort;
    private final ReportRecipientPort recipientPort;
    private final SlackReportSettings settings;
    private final Clock clock;

    @Override
    public void forwardNewReports() {
        String channelId = settings.channelId();

        Optional<String> cursor;
        try {
            cursor = cursorPort.getCursor(channelId);
        } catch (RuntimeException e) {
            // 커서를 읽지 못한 채로 "처음"으로 착각해 시작점을 옮기면 안 된다.
            log.warn("[SlackReport] 커서를 읽지 못해 이번 주기를 건너뜁니다: {}", e.toString());
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
            log.warn("[SlackReport] 채널 조회 실패, 다음 주기에 다시 시도합니다: {}", e.getMessage());
            return;
        }

        List<SlackChannelMessage> all = history.messagesOldestFirst();
        if (all.isEmpty()) {
            return;
        }

        List<SlackChannelMessage> forwardable =
                all.stream().filter(m -> !m.isSystemMessage()).toList();
        if (forwardable.isEmpty()) {
            // 시스템 메시지뿐이면 보낼 것이 없으니 수신자 조회 없이 커서만 옮긴다.
            saveCursor(channelId, all.get(all.size() - 1).ts());
            return;
        }

        List<String> recipients = loadRecipients();
        if (recipients.isEmpty()) {
            return;
        }
        int skipped = Math.max(0, forwardable.size() - settings.maxPerPoll());
        List<SlackChannelMessage> toForward = forwardable.subList(skipped, forwardable.size());

        if (skipped > 0 || (history.truncated() && !forwardable.isEmpty())) {
            sendToRecipients(recipients, summaryText(skipped, history.truncated()));
        }

        for (SlackChannelMessage message : toForward) {
            sendToRecipients(recipients, buildText(channelId, message));
            saveCursor(channelId, message.ts());
        }

        // 뒤에 시스템 메시지만 있었어도 다음 주기에 다시 가져오지 않도록 끝까지 옮긴다.
        saveCursor(channelId, all.get(all.size() - 1).ts());
        log.info(
                "[SlackReport] 새 글 {}건 확인, {}건 전달 (생략 {}건)", all.size(), toForward.size(), skipped);
    }

    private void startFromNow(String channelId) {
        String start;
        try {
            start = channelPort.latestTs(channelId).orElseGet(this::nowTs);
        } catch (SlackChannelException e) {
            log.warn("[SlackReport] 시작 지점을 정하지 못했습니다. 다음 주기에 다시 시도합니다: {}", e.getMessage());
            return;
        }
        saveCursor(channelId, start);
        log.info("[SlackReport] 커서가 없어 과거 글은 전달하지 않고 지금({})부터 시작합니다.", start);
    }

    private String nowTs() {
        Instant now = clock.instant();
        return String.format("%d.%06d", now.getEpochSecond(), now.getNano() / 1000);
    }

    private void saveCursor(String channelId, String ts) {
        try {
            cursorPort.saveCursor(channelId, ts);
        } catch (RuntimeException e) {
            log.error("[SlackReport] 커서 저장 실패(다음 주기에 같은 글이 다시 전달될 수 있음): {}", e.toString());
        }
    }

    /** 수신자를 못 구하면 빈 목록(이번 주기는 건너뜀). 부팅을 막지 않고 경고만 남긴다. */
    private List<String> loadRecipients() {
        try {
            List<String> recipients = recipientPort.findRecipientIntraIds();
            if (recipients.isEmpty()) {
                log.warn(
                        "[SlackReport] 수신자(ADMIN/MASTER 권한 유저)가 없어 전달을 보류합니다. 관리자가 생기면 다음 주기에 전달됩니다.");
            }
            return recipients;
        } catch (RuntimeException e) {
            log.warn("[SlackReport] 수신자를 조회하지 못해 이번 주기를 건너뜁니다: {}", e.toString());
            return List.of();
        }
    }

    private void sendToRecipients(List<String> recipients, String text) {
        for (String recipient : recipients) {
            try {
                alarmPort.sendDm(recipient, text);
            } catch (RuntimeException e) {
                log.error("[SlackReport] DM 전달 실패 - 수신자: {}, 원인: {}", recipient, e.toString());
            }
        }
    }

    private String summaryText(int skipped, boolean truncated) {
        String count = truncated ? skipped + "건 이상" : skipped + "건";
        return "📩 [사물함 오류제보] 새 글이 많아 이전 " + count + "은 전달을 생략했습니다. 채널에서 직접 확인해 주세요.";
    }

    private String buildText(String channelId, SlackChannelMessage message) {
        StringBuilder sb = new StringBuilder("📩 [사물함 오류제보] 새 글이 올라왔습니다.\n");
        sb.append("작성자: ")
                .append(message.user() != null ? "<@" + message.user() + ">" : "(알 수 없음)")
                .append('\n');
        formatTime(message.ts()).ifPresent(t -> sb.append("시각: ").append(t).append('\n'));
        sb.append("────────────\n");
        sb.append(prepareText(message.text())).append('\n');
        if (message.fileCount() > 0) {
            sb.append("📎 첨부 파일 ").append(message.fileCount()).append("개 (원문에서 확인)\n");
        }
        if (message.replyCount() > 0) {
            sb.append("💬 스레드 답글 ").append(message.replyCount()).append("개\n");
        }
        channelPort
                .permalink(channelId, message.ts())
                .ifPresent(link -> sb.append("원문: ").append(link));
        return sb.toString().stripTrailing();
    }

    private String prepareText(String raw) {
        String text = raw == null || raw.isBlank() ? "(본문 없음)" : raw;
        // 전체 멘션은 DM 에서 의미가 없으니 평문으로 바꾼다.
        text = text.replaceAll(MASS_MENTION, "@$1");
        if (text.length() > settings.maxTextLength()) {
            text = text.substring(0, settings.maxTextLength()) + "…(이하 원문에서 확인)";
        }
        return text;
    }

    private Optional<String> formatTime(String ts) {
        try {
            long seconds = Long.parseLong(ts.split("\\.")[0]);
            return Optional.of(TIME_FORMAT.format(Instant.ofEpochSecond(seconds)));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
