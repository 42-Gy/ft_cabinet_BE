package com.gyeongsan.cabinet.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity // DB 테이블과 매칭 클래스.
@Table(name = "USER") // 테이블 이름: USER
@Getter // lombok 패키지를 통해 모든 필드의 getter 자동 생성
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id // Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", length = 32, unique = true, nullable = false)
    private String name; // 인트라 ID

    @Column(name = "EMAIL", unique = true)
    private String email;

    @Enumerated(EnumType.STRING) // Enum 이름을 문자열("USER")로 저장
    @Column(name = "ROLE", nullable = false)
    private UserRole role;

    // 재화 시스템
    @Column(name = "COIN", nullable = false)
    private Long coin = 0L; // 기본값 0원

    @Column(name = "BLACKHOLED_AT")
    private LocalDateTime blackholedAt; // 블랙홀 날짜

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt; // 탈퇴 날짜

    // --- 알림 설정 ---
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
    }

    public static User of(String name, String email, UserRole role) {
        // 처음 가입할 때는 블랙홀 날짜(blackholedAt)를 모르니 null로 처리
        return new User(name, email, null, role);
    }

    public static User of(String name, String email, LocalDateTime blackholedAt, UserRole role) {
        return new User(name, email, blackholedAt, role);
    }

    // 👇 [추가] Blackhole 일자 업데이트 메서드 (42 API 동기화용)
    public void updateBlackholedAt(LocalDateTime blackholedAt) {
        this.blackholedAt = blackholedAt;
    }

    // --- 비즈니스 로직 (지갑 기능) ---

    // 돈 추가
    public void addCoin(Long amount) {
        this.coin += amount;
    }

    //  돈 쓰기 - 마이너스 방지 로직 처리!
    public void useCoin(Long amount) {
        if (this.coin < amount) {
            throw new IllegalArgumentException("코인이 부족합니다! (현재: " + this.coin + ")");
        }
        this.coin -= amount;
    }
}