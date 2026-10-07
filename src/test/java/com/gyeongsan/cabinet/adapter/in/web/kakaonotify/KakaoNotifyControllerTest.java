package com.gyeongsan.cabinet.adapter.in.web.kakaonotify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.adapter.in.web.kakaonotify.dto.KakaoNotifyConsentRequest;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyStatus;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.GrantKakaoNotifyConsentUseCase;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.KakaoNotifySettingsUseCase;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.GlobalExceptionHandler;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class KakaoNotifyControllerTest {

    private final GrantKakaoNotifyConsentUseCase grant =
            Mockito.mock(GrantKakaoNotifyConsentUseCase.class);
    private final KakaoNotifySettingsUseCase settings =
            Mockito.mock(KakaoNotifySettingsUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ObjectMapper[] holder = new ObjectMapper[1];
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(ctx -> holder[0] = ctx.getBean(ObjectMapper.class));
        mvc =
                MockMvcBuilders.standaloneSetup(new KakaoNotifyController(grant, settings))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(holder[0]))
                        .build();
        loginAs(9L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(long id) {
        User user = User.of("intra" + id, "intra" + id + "@example.com", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        UserPrincipal principal = new UserPrincipal(user, Map.of());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("동의 등록은 로그인한 본인의 ID 와 인가 코드로만 처리하고, 응답은 캐시하지 않는다")
    void consentUsesPrincipalOnly() throws Exception {
        given(grant.grantConsent(9L, "the-code")).willReturn(new KakaoNotifyStatus(true, true));

        mvc.perform(
                        post("/v4/users/me/kakao-notify/consent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"authorizationCode\":\"the-code\",\"userId\":1}"))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        "Cache-Control",
                                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.data.consented").value(true))
                .andExpect(jsonPath("$.data.alarmEnabled").value(true))
                .andExpect(jsonPath("$.data.receiving").value(true));

        verify(grant).grantConsent(9L, "the-code");
        verify(grant, never()).grantConsent(org.mockito.ArgumentMatchers.eq(1L), anyString());
    }

    @Test
    @DisplayName("서비스가 거부하면(계정 불일치 등) 그 오류 코드와 상태로 응답한다")
    void mapsServiceErrors() throws Exception {
        given(grant.grantConsent(anyLong(), anyString()))
                .willThrow(new ServiceException(ErrorCode.KAKAO_NOTIFY_ACCOUNT_MISMATCH));

        mvc.perform(
                        post("/v4/users/me/kakao-notify/consent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"authorizationCode\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(ErrorCode.KAKAO_NOTIFY_ACCOUNT_MISMATCH.getMessage()));
    }

    @Test
    @DisplayName("상태 조회와 알림 스위치 변경도 본인 ID 로만 처리한다")
    void statusAndAlarm() throws Exception {
        given(settings.getStatus(9L)).willReturn(new KakaoNotifyStatus(true, false));
        given(settings.setAlarm(9L, true)).willReturn(new KakaoNotifyStatus(true, true));

        mvc.perform(get("/v4/users/me/kakao-notify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiving").value(false));
        mvc.perform(
                        put("/v4/users/me/kakao-notify/alarm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"enabled\":true,\"userId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiving").value(true));

        verify(settings).setAlarm(9L, true);
    }

    @Test
    @DisplayName("스위치 값(enabled)이 없으면 서비스를 호출하지 않고 거부한다")
    void alarmRequiresEnabled() throws Exception {
        mvc.perform(
                        put("/v4/users/me/kakao-notify/alarm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().is4xxClientError());

        verify(settings, never()).setAlarm(anyLong(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    @DisplayName("요청 본문 객체의 문자열 표현에 인가 코드가 나오지 않는다(로그에 찍혀도 안전)")
    void requestToStringHasNoCode() throws Exception {
        KakaoNotifyConsentRequest request =
                new ObjectMapper()
                        .readValue(
                                "{\"authorizationCode\":\"secret-code\"}",
                                KakaoNotifyConsentRequest.class);

        assertThat(request.toString()).doesNotContain("secret-code");
    }

    @Test
    @DisplayName("컨트롤러는 /v4/auth/** 가 아닌 인증 필요 경로에 있다(보안 설정에서 /v4/auth/** 는 인증 없이 열려 있음)")
    void notUnderPermitAllAuthPath() {
        String base =
                KakaoNotifyController.class.getAnnotation(
                                org.springframework.web.bind.annotation.RequestMapping.class)
                        .value()[0];

        assertThat(base).startsWith("/v4/users/").doesNotStartWith("/v4/auth");
    }
}
