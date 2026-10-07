package com.gyeongsan.cabinet.domain.kakaonotify.port.out;

import java.util.Optional;

/** 공지 채널별로 "여기까지 확인했다"는 마지막 메시지 ts. 오류제보 커서와 키를 나눠 쓴다. */
public interface NoticeCursorPort {

    Optional<String> getCursor(String channelId);

    void saveCursor(String channelId, String ts);
}
