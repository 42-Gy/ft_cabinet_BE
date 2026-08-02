package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import java.time.LocalDateTime;

public record ReturnPhotoResponseDto(
        Long lentHistoryId,
        Integer cabinetVisibleNum,
        String userName,
        String photoUrl,
        LocalDateTime returnedAt,
        String returnMemo) {}
