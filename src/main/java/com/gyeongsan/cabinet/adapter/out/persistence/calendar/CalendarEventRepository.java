package com.gyeongsan.cabinet.adapter.out.persistence.calendar;

import com.gyeongsan.cabinet.domain.calendar.model.CalendarEvent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query(
            "SELECT e FROM CalendarEvent e JOIN FETCH e.announcer "
                    + "WHERE e.eventDate BETWEEN :start AND :end "
                    + "ORDER BY e.eventDate ASC")
    List<CalendarEvent> findAllByEventDateBetween(
            @Param("start") LocalDate start, @Param("end") LocalDate end);
}
