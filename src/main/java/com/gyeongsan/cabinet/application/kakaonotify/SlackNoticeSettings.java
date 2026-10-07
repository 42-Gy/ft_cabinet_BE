package com.gyeongsan.cabinet.application.kakaonotify;

/**
 * 공지 → 카카오톡 전달 설정.
 *
 * @param channelId 감시할 슬랙 공지 채널 ID
 * @param maxPerPoll 한 번에 보낼 최대 공지 수. 넘치면 오래된 것은 요약 한 줄로 대체한다
 * @param maxHoldHours 수신자가 없어 보류 중인 공지를 붙잡아 두는 최대 시간. 지나면 오래된 공지를 새 수신자에게 쏟아내지 않도록 건너뛴다
 * @param linkUrl 카톡 메시지를 누르면 열리는 주소(카카오 앱에 등록된 도메인)
 */
public record SlackNoticeSettings(
        String channelId, int maxPerPoll, int maxHoldHours, String linkUrl) {

    public SlackNoticeSettings {
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("공지 채널 ID(SLACK_NOTICE_CHANNEL_ID)가 필요합니다.");
        }
        if (maxPerPoll <= 0 || maxHoldHours <= 0) {
            throw new IllegalArgumentException("maxPerPoll, maxHoldHours 는 1 이상이어야 합니다.");
        }
        if (linkUrl == null || !(linkUrl.startsWith("https://") || linkUrl.startsWith("http://"))) {
            throw new IllegalArgumentException(
                    "카톡 메시지 링크(SLACK_NOTICE_LINK_URL 또는 FRONTEND_URL)는 http(s) 주소여야 합니다.");
        }
    }
}
