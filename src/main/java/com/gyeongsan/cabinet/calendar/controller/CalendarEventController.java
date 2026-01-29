package com.gyeongsan.cabinet.calendar.controller;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.calendar.dto.*;
import com.gyeongsan.cabinet.calendar.service.CalendarEventService;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.common.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v4")
@RequiredArgsConstructor
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @GetMapping("/calendar/events")
    public ApiResponse<List<CalendarEventResponseDto>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.success(calendarEventService.getEvents(start, end));
    }

    @PostMapping("/admin/calendar/events")
    public ApiResponse<MessageResponse> createEvent(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody CalendarEventRequestDto request) {
        calendarEventService.createEvent(userPrincipal.getUserId(), request);
        return ApiResponse.success(new MessageResponse("✅ 일정이 등록되었습니다."));
    }

    @PutMapping("/admin/calendar/events/{eventId}")
    public ApiResponse<MessageResponse> updateEvent(
            @PathVariable Long eventId,
            @RequestBody CalendarEventRequestDto request) {
        calendarEventService.updateEvent(eventId, request);
        return ApiResponse.success(new MessageResponse("✅ 일정이 수정되었습니다."));
    }

    @DeleteMapping("/admin/calendar/events/{eventId}")
    public ApiResponse<MessageResponse> deleteEvent(@PathVariable Long eventId) {
        calendarEventService.deleteEvent(eventId);
        return ApiResponse.success(new MessageResponse("✅ 일정이 삭제되었습니다."));
    }
}
