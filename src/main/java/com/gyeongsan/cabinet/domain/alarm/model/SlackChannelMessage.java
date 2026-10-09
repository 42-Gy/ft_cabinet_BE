package com.gyeongsan.cabinet.domain.alarm.model;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 슬랙 채널의 최상위 메시지 한 건.
 *
 * @param ts 채널 안에서 유일하고 시간순으로 커지는 메시지 식별자(예: "1696500000.000200")
 * @param user 작성자 슬랙 사용자 ID. 없으면 null
 * @param text 본문(슬랙 mrkdwn 원문)
 * @param subtype 메시지 하위 유형. 일반 글이면 null
 * @param replyCount 스레드 답글 수
 * @param fileCount 첨부 파일 수
 */
public record SlackChannelMessage(
        String ts, String user, String text, String subtype, int replyCount, int fileCount) {

    /** 슬랙이 자동으로 만드는 시스템 메시지(입퇴장, 주제 변경 등)를 가려내기 위한 하위 유형. */
    private static final Set<String> SYSTEM_SUBTYPES =
            Set.of(
                    "pinned_item",
                    "unpinned_item",
                    "message_changed",
                    "message_deleted",
                    "tombstone",
                    "reminder_add",
                    "ekm_access_denied",
                    "joiner_notification",
                    "joiner_notification_for_inviter",
                    "bot_add",
                    "bot_remove",
                    "bot_enable",
                    "bot_disable");

    private static final Pattern SYSTEM_PREFIX = Pattern.compile("^(channel_|group_).*");

    /** 사람이 쓴 글이든 봇이 쓴 글이든 시스템 메시지만 아니면 전달 대상이다. */
    public boolean isSystemMessage() {
        if (subtype == null || subtype.isEmpty()) {
            return false;
        }
        return SYSTEM_SUBTYPES.contains(subtype) || SYSTEM_PREFIX.matcher(subtype).matches();
    }
}
