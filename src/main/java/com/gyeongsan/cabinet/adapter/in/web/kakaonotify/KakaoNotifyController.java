package com.gyeongsan.cabinet.adapter.in.web.kakaonotify;

import com.gyeongsan.cabinet.adapter.in.web.kakaonotify.dto.KakaoNotifyAlarmRequest;
import com.gyeongsan.cabinet.adapter.in.web.kakaonotify.dto.KakaoNotifyConsentRequest;
import com.gyeongsan.cabinet.adapter.in.web.kakaonotify.dto.KakaoNotifyStatusResponse;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.GrantKakaoNotifyConsentUseCase;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.KakaoNotifySettingsUseCase;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 슬랙 공지를 카카오톡으로 받기 위한 동의/스위치. KAKAO_NOTIFY_ENABLED=true 일 때만 등록된다.
 *
 * <p>경로가 {@code /v4/auth/**} 가 아니라 {@code /v4/users/me/**} 인 이유: 보안 설정에서 {@code /v4/auth/**} 는 인증
 * 없이 열려 있어(컨트롤러가 로그인 상태를 직접 가정) 로그인하지 않은 요청이 그대로 들어올 수 있다. {@code /v4/users/**} 는 인증된 요청만 통과하므로, 보안
 * 설정을 건드리지 않고 로그인한 본인에게만 열리게 했다. 대상 유저는 항상 JWT 에서만 얻고 요청 본문의 값은 쓰지 않는다.
 */
@RestController
@RequestMapping("/v4/users/me/kakao-notify")
@ConditionalOnProperty(name = "app.kakao-notify.enabled", havingValue = "true")
@RequiredArgsConstructor
@RateLimiter(name = "userApi")
public class KakaoNotifyController {

    private final GrantKakaoNotifyConsentUseCase grantConsent;
    private final KakaoNotifySettingsUseCase settings;

    @GetMapping
    public ResponseEntity<ApiResponse<KakaoNotifyStatusResponse>> status(
            @AuthenticationPrincipal UserPrincipal principal) {
        return noStore(KakaoNotifyStatusResponse.from(settings.getStatus(principal.getUserId())));
    }

    /** 프론트가 scope=talk_message 로 받은 인가 코드를 보내 동의를 등록한다. */
    @PostMapping("/consent")
    public ResponseEntity<ApiResponse<KakaoNotifyStatusResponse>> consent(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody KakaoNotifyConsentRequest request) {
        return noStore(
                KakaoNotifyStatusResponse.from(
                        grantConsent.grantConsent(
                                principal.getUserId(), request.getAuthorizationCode())));
    }

    @PutMapping("/alarm")
    public ResponseEntity<ApiResponse<KakaoNotifyStatusResponse>> alarm(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody KakaoNotifyAlarmRequest request) {
        if (request.getEnabled() == null) {
            throw new IllegalArgumentException("enabled 값이 필요합니다.");
        }
        return noStore(
                KakaoNotifyStatusResponse.from(
                        settings.setAlarm(principal.getUserId(), request.getEnabled())));
    }

    private static ResponseEntity<ApiResponse<KakaoNotifyStatusResponse>> noStore(
            KakaoNotifyStatusResponse body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(body));
    }
}
