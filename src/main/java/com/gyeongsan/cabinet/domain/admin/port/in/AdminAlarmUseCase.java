package com.gyeongsan.cabinet.domain.admin.port.in;

public interface AdminAlarmUseCase {
    void sendEmergencyNotice(String message);
}
