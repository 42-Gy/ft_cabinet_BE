package com.gyeongsan.cabinet.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "USER")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", length = 32, unique = true, nullable = false)
    private String name;

    @Column(name = "EMAIL", unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false)
    private UserRole role;

    @Column(name = "COIN", nullable = false)
    private Long coin = 0L;

    @Column(name = "PENALTY_DAYS", nullable = false)
    private Integer penaltyDays = 0;

    @Column(name = "BLACKHOLED_AT")
    private LocalDateTime blackholedAt;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Column(name = "SLACK_ALARM")
    private boolean slackAlarm = true;

    @Column(name = "EMAIL_ALARM")
    private boolean emailAlarm = true;

    @Column(name = "PUSH_ALARM")
    private boolean pushAlarm = false;

    protected User(String name, String email, LocalDateTime blackholedAt, UserRole role) {
        this.name = name;
        this.email = email;
        this.blackholedAt = blackholedAt;
        this.role = role;
        this.coin = 0L;
        this.penaltyDays = 0;
    }

    public static User of(String name, String email, UserRole role) {
        return new User(name, email, null, role);
    }

    public static User of(String name, String email, LocalDateTime blackholedAt, UserRole role) {
        return new User(name, email, blackholedAt, role);
    }

    public void updateBlackholedAt(LocalDateTime blackholedAt) {
        this.blackholedAt = blackholedAt;
    }

    public void addCoin(Long amount) {
        this.coin += amount;
    }

    public void useCoin(Long amount) {
        if (this.coin < amount) {
            throw new IllegalArgumentException("코인이 부족합니다! (현재: " + this.coin + ")");
        }
        this.coin -= amount;
    }

    public void updatePenaltyDays(Integer newPenaltyDays) {
        if (newPenaltyDays == null || newPenaltyDays < 0) {
            this.penaltyDays = 0;
        } else {
            this.penaltyDays = newPenaltyDays;
        }
    }
}
