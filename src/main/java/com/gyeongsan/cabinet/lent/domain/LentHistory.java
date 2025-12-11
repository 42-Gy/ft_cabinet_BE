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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LENT_HISTORY_ID")
    private Long id;

    @Column(name = "STARTED_AT", nullable = false)
    private LocalDateTime startedAt; // 대여 시작 시간

    @Column(name = "EXPIRED_AT", nullable = false)
    private LocalDateTime expiredAt; // 반납 예정 시간

    @Column(name = "ENDED_AT")
    private LocalDateTime endedAt; // 실제 반납 시간 (null이면 아직 반납 x)

    // 추후에 ai로 1차 판별로 반납처리 가능 기능 어떻게 할지 미결정
//    @Column(name = "RETURN_PHOTO", length = 255)
//    private String returnPhoto;
    
    @ManyToOne(fetch = FetchType.LAZY) // 중요: LAZY 필요할 때만 유저 정보를 가져옵니다.
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) // 중요: LAZY 필요할 때만 사물함 정보를 가져옵니다.
    @JoinColumn(name = "CABINET_ID", nullable = false)
    private Cabinet cabinet;
    
    protected LentHistory(User user, Cabinet cabinet, LocalDateTime startedAt, LocalDateTime expiredAt) {
        this.user = user;
        this.cabinet = cabinet;
        this.startedAt = startedAt;
        this.expiredAt = expiredAt;
    }

    // 대여 시작할 때 호출
    public static LentHistory of(User user, Cabinet cabinet, LocalDateTime startedAt, LocalDateTime expiredAt) {
        return new LentHistory(user, cabinet, startedAt, expiredAt);
    }

    // --- 비즈니스 로직 ---

    // 반납 처리
    public void endLent(LocalDateTime now) {
        this.endedAt = now;
    }

    // 반납일이 없으면 대여중
    public boolean isEnded() {
        return this.endedAt != null;
    }

    // 반납 사진 저장메서드 (나중에 ai가 사진을 받을 떄 사용할 예정이니 보류 )
//    public void updateReturnPhoto(String photoUrl) {
//        this.returnPhoto = photoUrl;
//    }
}