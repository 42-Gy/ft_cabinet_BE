package com.gyeongsan.cabinet.adapter.out.persistence.calendar;

import com.gyeongsan.cabinet.calendar.domain.CalendarEvent;
import com.gyeongsan.cabinet.calendar.repository.CalendarEventRepository;
import com.gyeongsan.cabinet.domain.calendar.port.out.CalendarEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CalendarEventPersistenceAdapter implements CalendarEventRepositoryPort {

    private final CalendarEventRepository calendarEventRepository;

    @Override
    public List<CalendarEvent> findAllByEventDateBetween(LocalDate start, LocalDate end) {
        return calendarEventRepository.findAllByEventDateBetween(start, end);
    }

    @Override
    public Optional<CalendarEvent> findById(Long id) {
        return calendarEventRepository.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return calendarEventRepository.existsById(id);
    }

    @Override
    public CalendarEvent save(CalendarEvent event) {
        return calendarEventRepository.save(event);
    }

    @Override
    public void deleteById(Long id) {
        calendarEventRepository.deleteById(id);
    }
}
