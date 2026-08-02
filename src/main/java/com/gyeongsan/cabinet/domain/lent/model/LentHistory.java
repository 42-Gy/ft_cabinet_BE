package com.gyeongsan.cabinet.domain.lent.model;

import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.user.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "LENT_HISTORY",
        indexes = {
            @Index(name = "idx_lent_user_id", columnList = "USER_ID"),
            @Index(name = "idx_lent_cabinet_id", columnList = "CABINET_ID"),
            @Index(name = "idx_lent_ended_at", columnList = "ENDED_AT")
        })
@Getter
@ToString(exclude = {"user", "cabinet"})
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

    @Column(name = "RETURN_MEMO", length = 255)
    private String returnMemo;

    @Column(name = "IS_AUTO_EXTENSION", nullable = false)
    private boolean isAutoExtension = true;

    @Column(name = "PHOTO_URL")
    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CABINET_ID", nullable = false)
    private Cabinet cabinet;

    protected LentHistory(
            User user, Cabinet cabinet, LocalDateTime startedAt, LocalDateTime expiredAt) {
        this.user = user;
        this.cabinet = cabinet;
        this.startedAt = startedAt;
        this.expiredAt = expiredAt;
    }

    public static LentHistory of(
            User user, Cabinet cabinet, LocalDateTime startedAt, LocalDateTime expiredAt) {
        return new LentHistory(user, cabinet, startedAt, expiredAt);
    }

    public void endLent(LocalDateTime now, String returnMemo) {
        this.endedAt = now;
        this.returnMemo = returnMemo;
    }

    public void endLent(LocalDateTime now) {
        this.endedAt = now;
    }

    public void addReturnMemo(String returnMemo) {
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

    public void setAutoExtension(boolean isAutoExtension) {
        this.isAutoExtension = isAutoExtension;
    }

    public void attachReturnPhoto(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean isOverdue(LocalDateTime now) {
        if (isEnded()) return false;
        return now.isAfter(this.expiredAt);
    }

    public int calculateOverdueDays(LocalDateTime now) {
        if (!isOverdue(now)) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(this.expiredAt, now);
    }
}
