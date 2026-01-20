package com.gyeongsan.cabinet.admin.dto;

import java.time.LocalDate;

public record AttendanceStatResponse(
        LocalDate date,
        long count) {
}
