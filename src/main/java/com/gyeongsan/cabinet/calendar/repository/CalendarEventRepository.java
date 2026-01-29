package com.gyeongsan.cabinet.calendar.repository;

import com.gyeongsan.cabinet.calendar.domain.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query("SELECT e FROM CalendarEvent e JOIN FETCH e.announcer " +
            "WHERE e.eventDate BETWEEN :start AND :end " +
            "ORDER BY e.eventDate ASC")
    List<CalendarEvent> findAllByEventDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
