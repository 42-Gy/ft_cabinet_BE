package com.gyeongsan.cabinet.calendar.dto;

import java.time.LocalDate;

public record CalendarEventResponseDto(
        Long id,
        String title,
        String description,
        LocalDate eventDate,
        String announcerName) {
}
