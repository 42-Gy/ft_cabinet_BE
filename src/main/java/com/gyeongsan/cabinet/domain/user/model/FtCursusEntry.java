package com.gyeongsan.cabinet.domain.user.model;

/**
 * 42 API 의 cursus_users 한 항목에서 판정에 필요한 값만 뽑은 것.
 *
 * @param cursusId cursus.id (피시너 9, 본과정 21 등)
 * @param grade 해당 cursus 에서의 등급 원문. 없으면 null
 */
public record FtCursusEntry(int cursusId, String grade) {}
