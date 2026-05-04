package com.gyeongsan.cabinet.domain.alarm.port.out;

public interface AlarmPort {

    void sendDm(String intraId, String message);
}
