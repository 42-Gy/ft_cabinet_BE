package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

/** Undo 요청. reason 은 되돌리는 이유로 필수이며 감사 로그에 남는다. */
public record UndoRequest(String reason) {}
