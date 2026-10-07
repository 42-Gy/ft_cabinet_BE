package com.gyeongsan.cabinet.adapter.out.persistence.kakaonotify;

import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyConsent;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoRecipient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KakaoNotifyConsentRepository extends JpaRepository<KakaoNotifyConsent, Long> {

    @Query("SELECT c FROM KakaoNotifyConsent c WHERE c.user.id = :userId")
    Optional<KakaoNotifyConsent> findByUserId(@Param("userId") Long userId);

    String RECIPIENT_SELECT =
            "SELECT new com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoRecipient("
                    + "u.id, u.name, c.encryptedRefreshToken) "
                    + "FROM KakaoNotifyConsent c JOIN c.user u "
                    + "WHERE c.revokedAt IS NULL AND u.kakaoAlarm = true AND u.deletedAt IS NULL";

    @Query(RECIPIENT_SELECT + " ORDER BY u.id")
    List<KakaoRecipient> findActiveRecipients();

    @Query(RECIPIENT_SELECT + " AND u.id = :userId")
    Optional<KakaoRecipient> findActiveRecipient(@Param("userId") Long userId);

    /** 읽은 토큰이 그대로일 때만 해지한다(조건부 갱신 한 문장이라 동시 재동의와 겹쳐도 안전하다). */
    @Modifying(clearAutomatically = true)
    @Query(
            "UPDATE KakaoNotifyConsent c SET c.revokedAt = :now "
                    + "WHERE c.user.id = :userId AND c.revokedAt IS NULL "
                    + "AND c.encryptedRefreshToken = :expected")
    int revokeIfTokenUnchanged(
            @Param("userId") Long userId,
            @Param("expected") String expectedEncryptedToken,
            @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query(
            "UPDATE KakaoNotifyConsent c SET c.encryptedRefreshToken = :newToken "
                    + "WHERE c.user.id = :userId AND c.revokedAt IS NULL "
                    + "AND c.encryptedRefreshToken = :expected")
    int rotateIfTokenUnchanged(
            @Param("userId") Long userId,
            @Param("expected") String expectedEncryptedToken,
            @Param("newToken") String newEncryptedToken);
}
