package com.gyeongsan.cabinet.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "유저를 찾을 수 없습니다."),
    NOT_ENOUGH_COIN(HttpStatus.BAD_REQUEST, "USER_002", "씨앗이 부족합니다."),

    CABINET_NOT_FOUND(HttpStatus.NOT_FOUND, "CABINET_001", "사물함을 찾을 수 없습니다."),
    INVALID_CABINET_STATUS(HttpStatus.BAD_REQUEST, "CABINET_002", "사용할 수 없는 사물함 상태입니다."),
    CABINET_ALREADY_RESERVED(HttpStatus.BAD_REQUEST, "CABINET_003", "다른 사용자가 이미 선점한 사물함입니다."),

    LENT_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "LENT_001", "이미 대여 중인 사물함이 있습니다."),
    LENT_NOT_FOUND(HttpStatus.NOT_FOUND, "LENT_002", "대여 기록이 없습니다."),
    BLACKHOLED_USER(HttpStatus.FORBIDDEN, "LENT_003", "블랙홀 유저는 대여할 수 없습니다."),
    PENALTY_USER(HttpStatus.FORBIDDEN, "LENT_004", "패널티 기간입니다."),
    SAME_CABINET_SWAP(HttpStatus.BAD_REQUEST, "LENT_005", "현재 사용 중인 사물함과 같은 곳으로 이사할 수 없습니다."),
    OVERDUE_USER_CANNOT_SWAP(HttpStatus.FORBIDDEN, "LENT_006", "연체 중에는 이사할 수 없습니다. 먼저 반납해주세요."),
    ALREADY_RENTING_CANNOT_RESERVE(HttpStatus.BAD_REQUEST, "LENT_007", "이미 대여 중인 사용자는 예약할 수 없습니다."),
    ALREADY_RESERVED(HttpStatus.BAD_REQUEST, "LENT_008", "이미 예약 중인 사물함이 있습니다."),

    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ITEM_001", "아이템이 존재하지 않습니다."),
    LENT_TICKET_NOT_FOUND(HttpStatus.BAD_REQUEST, "ITEM_002", "대여권이 부족합니다."),
    EXTENSION_TICKET_NOT_FOUND(HttpStatus.BAD_REQUEST, "ITEM_003", "연장권이 부족합니다."),
    SWAP_TICKET_NOT_FOUND(HttpStatus.BAD_REQUEST, "ITEM_004", "이사권이 부족합니다."),
    PENALTY_EXEMPTION_TICKET_NOT_FOUND(HttpStatus.BAD_REQUEST, "ITEM_005", "패널티 감면권이 부족합니다."),
    PENALTY_NOT_FOUND(HttpStatus.BAD_REQUEST, "ITEM_006", "적용된 패널티가 없습니다."),

    AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI_001",
            "AI 검사 서버 오류로 반납이 보류되었습니다. 홈페이지 공지된 연락처로 연락 주시면 감사하겠습니다."),
    CABINET_NOT_EMPTY(HttpStatus.BAD_REQUEST, "AI_002", "사물함 안에 물품이 감지되었습니다. 물품 수거 후 다시 사진을 찍어주세요."),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "AI_003", "사물함 사진이 아닙니다. 올바른 사진을 촬영해주세요."),

    EXTENSION_ITEM_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "ITEM_007", "연장권 보유 개수 초과입니다."),
    EXTENSION_ITEM_PURCHASE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "ITEM_008", "연장권 월간 구매 한도 초과입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
