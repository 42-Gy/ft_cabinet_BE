package com.gyeongsan.cabinet.domain.calendar.service;

import com.gyeongsan.cabinet.calendar.domain.CalendarEvent;
import com.gyeongsan.cabinet.calendar.dto.CalendarEventRequestDto;
import com.gyeongsan.cabinet.calendar.dto.CalendarEventResponseDto;
import com.gyeongsan.cabinet.domain.calendar.port.in.CalendarUseCase;
import com.gyeongsan.cabinet.domain.calendar.port.out.CalendarEventRepositoryPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarDomainService implements CalendarUseCase {

    private final CalendarEventRepositoryPort calendarEventRepository;
    private final UserRepositoryPort userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventResponseDto> getEvents(LocalDate start, LocalDate end) {
        return calendarEventRepository.findAllByEventDateBetween(start, end)
                .stream()
                .map(e -> new CalendarEventResponseDto(
                        e.getId(),
                        e.getTitle(),
                        e.getDescription(),
                        e.getEventDate(),
                        e.getAnnouncer().getName()))
                .collect(Collectors.toList());
    }

    @Override
    public void createEvent(Long adminId, CalendarEventRequestDto request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        CalendarEvent event = new CalendarEvent(
                request.title(),
                request.description(),
                request.eventDate(),
                admin);
        calendarEventRepository.save(event);
    }

    @Override
    public void updateEvent(Long eventId, CalendarEventRequestDto request) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));
        event.update(request.title(), request.description(), request.eventDate());
    }

    @Override
    public void deleteEvent(Long eventId) {
        if (!calendarEventRepository.existsById(eventId)) {
            throw new ServiceException(ErrorCode.ITEM_NOT_FOUND);
        }
        calendarEventRepository.deleteById(eventId);
    }
}
