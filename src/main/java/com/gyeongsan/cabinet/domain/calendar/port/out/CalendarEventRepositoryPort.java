package com.gyeongsan.cabinet.domain.calendar.port.out;

import com.gyeongsan.cabinet.calendar.domain.CalendarEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepositoryPort {

    List<CalendarEvent> findAllByEventDateBetween(LocalDate start, LocalDate end);

    Optional<CalendarEvent> findById(Long id);

    boolean existsById(Long id);

    CalendarEvent save(CalendarEvent event);

    void deleteById(Long id);
}
