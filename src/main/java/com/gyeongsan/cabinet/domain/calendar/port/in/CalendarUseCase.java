package com.gyeongsan.cabinet.domain.calendar.port.in;

import com.gyeongsan.cabinet.calendar.dto.CalendarEventRequestDto;
import com.gyeongsan.cabinet.calendar.dto.CalendarEventResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface CalendarUseCase {

    List<CalendarEventResponseDto> getEvents(LocalDate start, LocalDate end);

    void createEvent(Long adminId, CalendarEventRequestDto request);

    void updateEvent(Long eventId, CalendarEventRequestDto request);

    void deleteEvent(Long eventId);
}
