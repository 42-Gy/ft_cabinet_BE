package com.gyeongsan.cabinet.domain.admin.model;

/** 관리자 작업이 건드린 대상의 종류. 값은 DB 에 문자열로 저장되므로 이름을 바꾸지 않는다. */
public enum AdminActionTargetType {
    CABINET,
    LENT_HISTORY,
    USER
}
