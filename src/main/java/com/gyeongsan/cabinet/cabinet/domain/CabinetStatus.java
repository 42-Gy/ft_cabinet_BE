package com.gyeongsan.cabinet.cabinet.domain;

public enum CabinetStatus {
    AVAILABLE,      // 사용 가능
    FULL,           // 사용 중 (최대 인원)
    OVERDUE,        // 연체
    BROKEN,         // 고장으로 인한 점검 중 (수리 필요)
    DISABLED        // 👈 [추가] 사용 불가 상태 (영구적 폐쇄 또는 관리자 조치)
}