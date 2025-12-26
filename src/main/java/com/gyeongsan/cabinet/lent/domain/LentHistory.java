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
@Table(name = "LENT_HISTORY", indexes = {
        @Index(name = "idx_lent_user_id", columnList = "USER_ID"),
        @Index(name = "idx_lent_cabinet_id", columnList = "CABINET_ID"),
        @Index(name = "idx_lent_ended_at", columnList = "ENDED_AT")
})
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

    @Column(name = "RETURN_MEMO", length = 64)
    private String returnMemo;

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

    public void endLent(LocalDateTime now, String returnMemo) {
        this.endedAt = now;
        this.returnMemo = returnMemo;
    }

    public void endLent(LocalDateTime now) {
        this.endedAt = now;
    }

    public void setReturnMemo(String returnMemo) {
        this.returnMemo = returnMemo;
    }

    public boolean isEnded() {
        return this.endedAt != null;
    }

    public void extendExpiration(Long days) {
        if (this.expiredAt != null) {
            this.expiredAt = this.expiredAt.plusDays(days);
        }
    }
}
