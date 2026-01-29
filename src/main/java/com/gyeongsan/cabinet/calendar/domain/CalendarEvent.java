package com.gyeongsan.cabinet.calendar.domain;

import com.gyeongsan.cabinet.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CALENDAR_EVENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "EVENT_DATE", nullable = false)
    private LocalDate eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ANNOUNCER_ID", nullable = false)
    private User announcer;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CalendarEvent(String title, String description, LocalDate eventDate, User announcer) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.announcer = announcer;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String title, String description, LocalDate eventDate) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
    }
}
