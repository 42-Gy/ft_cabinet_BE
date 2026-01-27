package com.gyeongsan.cabinet.coin.domain;

import com.gyeongsan.cabinet.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "COIN_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoinHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "AMOUNT", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, length = 32)
    private CoinLogType type;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    protected CoinHistory(User user, Long amount, CoinLogType type, String description, LocalDateTime createdAt) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static CoinHistory of(User user, Long amount, CoinLogType type, String description) {
        return new CoinHistory(user, amount, type, description, LocalDateTime.now());
    }
}
