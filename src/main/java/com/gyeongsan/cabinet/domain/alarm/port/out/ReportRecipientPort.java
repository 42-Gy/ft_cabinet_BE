package com.gyeongsan.cabinet.domain.alarm.port.out;

import java.util.List;

/** 오류제보 DM 을 받을 관리자. 슬랙 DM 대상 식별자는 인트라 ID 다(AlarmPort.sendDm 과 같다). */
public interface ReportRecipientPort {

    /** 지금 시점의 수신자 인트라 ID. 없으면 빈 목록. 조회할 수 없으면 예외를 던진다. */
    List<String> findRecipientIntraIds();
}
