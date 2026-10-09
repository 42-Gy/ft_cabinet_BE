package com.gyeongsan.cabinet.application.alarm;

/**
 * 오류제보 전달 설정.
 *
 * @param channelId 감시할 슬랙 채널 ID(C... / G...)
 * @param maxPerPoll 한 번에 전달할 최대 메시지 수. 넘치면 오래된 것은 요약 한 줄로 대체한다
 * @param maxTextLength DM 에 싣는 본문 최대 길이
 */
public record SlackReportSettings(String channelId, int maxPerPoll, int maxTextLength) {

    public SlackReportSettings {
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("오류제보 채널 ID(SLACK_REPORT_CHANNEL_ID)가 필요합니다.");
        }
        if (maxPerPoll <= 0 || maxTextLength <= 0) {
            throw new IllegalArgumentException("maxPerPoll, maxTextLength 는 1 이상이어야 합니다.");
        }
    }
}
