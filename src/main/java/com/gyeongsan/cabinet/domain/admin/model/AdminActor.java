package com.gyeongsan.cabinet.domain.admin.model;

/** 관리자 작업을 수행한 사람. 이름은 작업 시점의 값을 로그에 남기기 위해 함께 받는다. */
public record AdminActor(Long id, String name) {

    public AdminActor {
        if (id == null) {
            throw new IllegalArgumentException("관리자 ID 는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("관리자 이름은 필수입니다.");
        }
    }
}
