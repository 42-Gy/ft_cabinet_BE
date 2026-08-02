package com.gyeongsan.cabinet.adapter.in.web.calendar.dto;

import java.time.LocalDate;

public record CalendarEventRequestDto(String title, String description, LocalDate eventDate) {}
