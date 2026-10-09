package com.gyeongsan.cabinet.domain.admin.model;

/** Undo 를 거부하는 이유. 응답에 문자열로 내려가므로 이름을 바꾸지 않는다. */
public enum UndoConflictCode {
    /** 사물함의 현재 상태가 원래 작업 직후와 달라졌다. */
    CABINET_CHANGED,
    /** 되살릴 대여가 없거나, 원래 작업 직후와 달라졌다. */
    LENT_CHANGED,
    /** 그 사물함에 지금 다른 활성 대여가 있다. */
    CABINET_OCCUPIED,
    /** 되살릴 대여의 사용자가 지금 다른 활성 대여를 가지고 있다. */
    USER_HAS_ACTIVE_LENT,
    /** 되살릴 대여의 만료일이 이미 지났다. 되살리면 연체 패널티가 붙는다. */
    LENT_EXPIRED,
    /** 그 사물함에 이사권 예약이 걸려 있다. */
    RESERVED,
    /** 되살릴 대여의 사용자가 없거나 삭제되었다. */
    USER_INACTIVE,
    /** 이미 되돌려진 작업이다. */
    ALREADY_UNDONE
}
