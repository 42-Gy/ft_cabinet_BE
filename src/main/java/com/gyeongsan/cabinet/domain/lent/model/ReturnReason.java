package com.gyeongsan.cabinet.domain.lent.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReturnReason {
    ADMIN_FORCE("ADMIN_FORCE"),
    BLACKHOLE("BLACKHOLE");

    private final String value;
}
