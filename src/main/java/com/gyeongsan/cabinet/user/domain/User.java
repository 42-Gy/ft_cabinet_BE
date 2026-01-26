package com.gyeongsan.cabinet.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "USER")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Builder.Default
    @Version
    @Column(name = "version")
    private Long version = 0L;

    @Column(name = "NAME", length = 32, unique = true, nullable = false)
    private String name;

    @Column(name = "EMAIL", unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false)
    private UserRole role;

    @Builder.Default
    @Column(name = "COIN", nullable = false)
    private Long coin = 0L;

    @Builder.Default
    @Column(name = "PENALTY_DAYS", nullable = false)
    private Integer penaltyDays = 0;

    @Builder.Default
    @Column(name = "MONTHLY_LOGTIME", nullable = false)
    private Integer monthlyLogtime = 0;

    @Column(name = "BLACKHOLED_AT")
    private LocalDateTime blackholedAt;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(name = "SLACK_ALARM")
    private boolean slackAlarm = true;

    @Builder.Default
    @Column(name = "EMAIL_ALARM")
    private boolean emailAlarm = true;

    @Builder.Default
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

    public void clearPenalty() {
        this.penaltyDays = 0;
    }

    public void addMonthlyLogtime(int minutes) {
        this.monthlyLogtime += minutes;
    }

    public void resetMonthlyLogtime() {
        this.monthlyLogtime = 0;
    }

    public void updateMonthlyLogtime(Integer monthlyLogtime) {
        if (monthlyLogtime == null || monthlyLogtime < 0) {
            this.monthlyLogtime = 0;
        } else {
            this.monthlyLogtime = monthlyLogtime;
        }
    }

    public void updateRole(UserRole role) {
        this.role = role;
    }
}
