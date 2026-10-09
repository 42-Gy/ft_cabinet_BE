package com.gyeongsan.cabinet.application.kakaonotify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyStatus;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoConsentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KakaoNotifySettingsServiceTest {

    private final KakaoConsentRepositoryPort consents = mock(KakaoConsentRepositoryPort.class);
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final KakaoNotifySettingsService service =
            new KakaoNotifySettingsService(consents, users);

    private User user() {
        User user = User.of("intra1", "a@x.kr", UserRole.USER);
        when(users.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    @DisplayName("동의가 있으면 알림을 켜고 끌 수 있다")
    void togglesWithConsent() {
        User user = user();
        when(consents.findStatus(1L)).thenReturn(new KakaoNotifyStatus(true, false));

        KakaoNotifyStatus on = service.setAlarm(1L, true);
        assertThat(user.isKakaoAlarm()).isTrue();
        assertThat(on.receiving()).isTrue();

        KakaoNotifyStatus off = service.setAlarm(1L, false);
        assertThat(user.isKakaoAlarm()).isFalse();
        assertThat(off.receiving()).isFalse();
        assertThat(off.consented()).isTrue(); // 동의는 남아 있고 스위치만 꺼짐
        verify(users, org.mockito.Mockito.times(2)).save(user);
    }

    @Test
    @DisplayName("동의가 없으면 알림을 켤 수 없지만, 끄는 것은 항상 가능하다")
    void cannotEnableWithoutConsent() {
        User user = user();
        user.updateKakaoAlarm(true);
        when(consents.findStatus(1L)).thenReturn(new KakaoNotifyStatus(false, true));

        assertThatThrownBy(() -> service.setAlarm(1L, true))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.KAKAO_NOTIFY_CONSENT_REQUIRED));

        assertThat(service.setAlarm(1L, false).alarmEnabled()).isFalse();
        assertThat(user.isKakaoAlarm()).isFalse();
    }

    @Test
    @DisplayName("없는 유저면 USER_NOT_FOUND")
    void unknownUser() {
        when(users.findByIdWithLock(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setAlarm(2L, true))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
