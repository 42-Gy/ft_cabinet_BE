package com.gyeongsan.cabinet.domain.kakaonotify.port.in;

public interface ForwardSlackNoticesUseCase {

    /** 슬랙 공지 채널의 새 글을 카카오 알림 수신 대상 전원에게 보낸다. 주기적으로 호출된다. */
    void forwardNewNotices();
}
