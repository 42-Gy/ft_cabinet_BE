package com.gyeongsan.cabinet.domain.kakaonotify.port.out;

import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyStatus;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoRecipient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KakaoConsentRepositoryPort {

    /**
     * 동의를 저장한다(없으면 만들고, 있으면 토큰을 바꾼다). 같은 유저의 동시 요청은 유저 행 락으로 한 줄씩 처리한다. 첫 동의이거나 해지된 동의를 되살린 경우에만 알림
     * 스위치를 켠다(이미 유효한 동의를 다시 받았을 때 유저가 꺼 둔 스위치를 되돌리지 않는다).
     *
     * @return 알림 스위치를 이번에 켰으면 true
     */
    boolean grant(Long userId, String encryptedRefreshToken, String scope, LocalDateTime now);

    /** 내 상태. 동의 행이 없으면 consented=false. */
    KakaoNotifyStatus findStatus(Long userId);

    /** 동의가 유효하고(해지되지 않음) 알림이 켜졌고 탈퇴하지 않은 유저 전원. */
    List<KakaoRecipient> findActiveRecipients();

    /** 발송 직전 재확인용. 위 조건을 지금도 만족하는 유저만 돌려준다(최신 토큰 포함). */
    Optional<KakaoRecipient> findActiveRecipient(Long userId);

    /**
     * 사용한 토큰({@code expectedEncryptedToken})이 아직 그대로일 때만 해지 처리한다. 그 사이 유저가 다시 동의해 토큰이 바뀌었다면 새 동의를
     * 해지하지 않는다.
     *
     * @return 실제로 해지했으면 true
     */
    boolean revokeIfTokenUnchanged(Long userId, String expectedEncryptedToken, LocalDateTime now);

    /**
     * refresh_token 이 회전됐을 때 저장한다. 읽은 값({@code expectedEncryptedToken})이 그대로일 때만 바꾼다(동시에 다른 곳이 먼저
     * 바꿨거나 재동의가 있었다면 건드리지 않는다).
     *
     * @return 실제로 바꿨으면 true
     */
    boolean rotateIfTokenUnchanged(
            Long userId, String expectedEncryptedToken, String newEncryptedToken);
}
