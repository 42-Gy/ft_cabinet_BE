package com.gyeongsan.cabinet.lent.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReturnReason {
    ADMIN_FORCE("ADMIN_FORCE"),
    BLACKHOLE("BLACKHOLE");

    private final String value;
}
