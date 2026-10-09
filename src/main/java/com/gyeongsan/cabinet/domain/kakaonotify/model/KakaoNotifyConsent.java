package com.gyeongsan.cabinet.domain.kakaonotify.model;

import com.gyeongsan.cabinet.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 talk_message 동의. 유저당 한 행(user_id UNIQUE)이다. 로그인용 OauthLink 와 일부러 분리했다.
 *
 * <p>refresh_token 은 암호화된 값만 들고 있다(평문은 이 객체에 닿지 않는다). 동의가 해지되면 {@code revokedAt} 이 채워지고, 다시 동의하면 같은
 * 행을 되살린다.
 */
@Entity
@Table(
        name = "KAKAO_NOTIFY_CONSENT",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_kakao_notify_consent_user",
                    columnNames = {"USER_ID"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KakaoNotifyConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "ENCRYPTED_REFRESH_TOKEN", nullable = false, length = 1024)
    private String encryptedRefreshToken;

    @Column(name = "SCOPE", nullable = false, length = 255)
    private String scope;

    @Column(name = "CONSENTED_AT", nullable = false)
    private LocalDateTime consentedAt;

    @Column(name = "REVOKED_AT")
    private LocalDateTime revokedAt;

    public static KakaoNotifyConsent create(
            User user, String encryptedRefreshToken, String scope, LocalDateTime now) {
        KakaoNotifyConsent consent = new KakaoNotifyConsent();
        consent.user = user;
        consent.encryptedRefreshToken = encryptedRefreshToken;
        consent.scope = scope;
        consent.consentedAt = now;
        return consent;
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    /**
     * 동의를 새로 받았다. 해지된 상태였다면 되살리고(동의 시각 갱신), 이미 유효했다면 토큰과 범위만 바꾼다.
     *
     * @return 이번 호출로 해지 → 유효가 되었으면 true(첫 동의가 아니라 "되살림")
     */
    public boolean renew(String encryptedRefreshToken, String scope, LocalDateTime now) {
        boolean reactivated = revokedAt != null;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.scope = scope;
        if (reactivated) {
            this.revokedAt = null;
            this.consentedAt = now;
        }
        return reactivated;
    }
}
