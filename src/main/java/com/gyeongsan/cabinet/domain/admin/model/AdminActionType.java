package com.gyeongsan.cabinet.domain.admin.model;

/** 감사 로그에 기록되는 관리자 작업 종류. 값은 DB 에 문자열로 저장되므로 이름을 바꾸지 않는다. */
public enum AdminActionType {
    CABINET_BULK_STATUS_UPDATE,
    CABINET_BULK_STATUS_UNDO
}
