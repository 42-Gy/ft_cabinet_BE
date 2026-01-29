package com.gyeongsan.cabinet.calendar.service;

import com.gyeongsan.cabinet.calendar.domain.CalendarEvent;
import com.gyeongsan.cabinet.calendar.dto.*;
import com.gyeongsan.cabinet.calendar.repository.CalendarEventRepository;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;

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

    public void updateEvent(Long eventId, CalendarEventRequestDto request) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));
        event.update(request.title(), request.description(), request.eventDate());
    }

    public void deleteEvent(Long eventId) {
        if (!calendarEventRepository.existsById(eventId)) {
            throw new ServiceException(ErrorCode.ITEM_NOT_FOUND);
        }
        calendarEventRepository.deleteById(eventId);
    }
}
