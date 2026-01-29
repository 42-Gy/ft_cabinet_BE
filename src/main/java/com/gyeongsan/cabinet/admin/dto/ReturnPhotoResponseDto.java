package com.gyeongsan.cabinet.admin.dto;

import java.time.LocalDateTime;

public record ReturnPhotoResponseDto(
        Long lentHistoryId,
        Integer cabinetVisibleNum,
        String userName,
        String photoUrl,
        LocalDateTime returnedAt,
        String returnMemo) {
}
