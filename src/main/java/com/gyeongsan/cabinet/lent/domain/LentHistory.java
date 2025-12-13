package com.gyeongsan.cabinet.lent.domain;

import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "LENT_HISTORY")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LENT_HISTORY_ID")
    private Long id;

    @Column(name = "STARTED_AT", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "EXPIRED_AT", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "ENDED_AT")
    private LocalDateTime endedAt;

    // 추후 AI 반납 기능 도입 시 주석 해제 예정
    // @Column(name = "RETURN_PHOTO", length = 255)
    // private String returnPhoto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CABINET_ID", nullable = false)
    private Cabinet cabinet;

    protected LentHistory(User user, Cabinet cabinet, LocalDateTime startedAt, LocalDateTime expiredAt) {
        this.user = user;
        this.cabinet = cabinet;
        this.startedAt = startedAt;
        this.expiredAt = expiredAt;
    }

    public static LentHistory of(User user, Cabinet cabinet, LocalDateTime startedAt, LocalDateTime expiredAt) {
        return new LentHistory(user, cabinet, startedAt, expiredAt);
    }

    public void endLent(LocalDateTime now) {
        this.endedAt = now;
    }

    public boolean isEnded() {
        return this.endedAt != null;
    }

    public void extendExpiration(Long days) {
        if (this.expiredAt != null) {
            this.expiredAt = this.expiredAt.plusDays(days);
        }
    }

    // 나중에 AI 반납 사진 저장용 메서드 (보류)
    // public void updateReturnPhoto(String photoUrl) {
    //     this.returnPhoto = photoUrl;
    // }
}