package com.gyeongsan.cabinet.domain.user.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "BANNED_USER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BannedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "INTRA_ID", length = 32, unique = true, nullable = false)
    private String intraId;

    @Column(name = "REASON", length = 255)
    private String reason;

    @Column(name = "BANNED_AT", nullable = false)
    private LocalDateTime bannedAt;

    public BannedUser(String intraId, String reason) {
        this.intraId = intraId;
        this.reason = reason;
        this.bannedAt = LocalDateTime.now();
    }

    public static BannedUser of(String intraId, String reason) {
        return new BannedUser(intraId, reason);
    }
}
