package com.gyeongsan.cabinet.domain.lent.model;

import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.user.model.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    /** 종료된 대여를 다시 활성 상태로 되돌린다(관리자 Undo). 종료 시각을 지우고 반납 메모를 지정한 값으로 복원한다. */
    public void reopen(String returnMemo) {
        this.endedAt = null;
        this.returnMemo = returnMemo;
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

    /**
     * 만료일까지 남은 일수를 달력 날짜 기준으로 계산한다. 시각은 보지 않는다.
     *
     * <p>양수는 남은 일수, 0은 오늘이 만료일, 음수는 만료일이 지난 일수다. 반납 임박 알림(D-7, D-1)이 만료일의 날짜로 일수를 세는 것과 같은 기준이다.
     */
    public int calculateRemainingDays(LocalDate today) {
        return (int) ChronoUnit.DAYS.between(today, this.expiredAt.toLocalDate());
    }
}
