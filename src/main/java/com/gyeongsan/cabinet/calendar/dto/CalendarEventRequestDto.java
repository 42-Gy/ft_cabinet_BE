package com.gyeongsan.cabinet.calendar.dto;

import java.time.LocalDate;

public record CalendarEventRequestDto(
        String title,
        String description,
        LocalDate eventDate) {
}
