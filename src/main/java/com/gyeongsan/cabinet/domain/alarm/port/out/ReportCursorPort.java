package com.gyeongsan.cabinet.domain.alarm.port.out;

import java.util.Optional;

/** 채널별로 "여기까지 확인했다"는 마지막 메시지 ts 를 보관한다. */
public interface ReportCursorPort {

    Optional<String> getCursor(String channelId);

    void saveCursor(String channelId, String ts);
}
