package com.gyeongsan.cabinet.domain.alarm.port.in;

public interface ForwardSlackReportsUseCase {

    /** 오류제보 채널의 새 글을 설정된 관리자에게 DM 으로 전달한다. 주기적으로 호출된다. */
    void forwardNewReports();
}
