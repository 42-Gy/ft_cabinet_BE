package com.gyeongsan.cabinet.application.kakaonotify;

import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyStatus;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.KakaoNotifySettingsUseCase;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoConsentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class KakaoNotifySettingsService implements KakaoNotifySettingsUseCase {

    private final KakaoConsentRepositoryPort consentRepository;
    private final UserRepositoryPort userRepository;

    @Override
    public KakaoNotifyStatus getStatus(Long userId) {
        return consentRepository.findStatus(userId);
    }

    /** 유저 행을 잠그고 바꾼다(다른 유저 갱신과 같은 순서, 동시 요청도 한 줄씩). */
    @Override
    @Transactional
    public KakaoNotifyStatus setAlarm(Long userId, boolean enabled) {
        User user =
                userRepository
                        .findByIdWithLock(userId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (enabled && !consentRepository.findStatus(userId).consented()) {
            throw new ServiceException(ErrorCode.KAKAO_NOTIFY_CONSENT_REQUIRED);
        }
        user.updateKakaoAlarm(enabled);
        userRepository.save(user);
        return new KakaoNotifyStatus(consentRepository.findStatus(userId).consented(), enabled);
    }
}
