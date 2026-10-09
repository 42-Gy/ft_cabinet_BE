package com.gyeongsan.cabinet.adapter.in.web.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.adapter.in.web.chatbot.dto.ChatbotDtos.PersonalRequest;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import com.gyeongsan.cabinet.domain.chatbot.port.in.PersonalChatbotUseCase;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.GlobalExceptionHandler;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestBody;

class PersonalChatbotControllerTest {

    private final PersonalChatbotUseCase useCase = Mockito.mock(PersonalChatbotUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ObjectMapper[] holder = new ObjectMapper[1];
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(ctx -> holder[0] = ctx.getBean(ObjectMapper.class));
        mvc =
                MockMvcBuilders.standaloneSetup(new PersonalChatbotController(useCase))
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
    @DisplayName("인증된 본인의 ID 로 조회하고, 응답은 캐시하지 않는다")
    void answersForPrincipal() throws Exception {
        given(useCase.answer(9L, PersonalIntent.PENALTY_STATUS))
                .willReturn(
                        new PersonalAnswer(
                                PersonalIntent.PENALTY_STATUS,
                                "패널티가 3일 남아 있어요.",
                                new PersonalFacts.PenaltyStatus(3, LocalDate.of(2026, 10, 10))));

        mvc.perform(
                        post("/v4/chatbot/personal")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"intent\":\"PENALTY_STATUS\"}"))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        "Cache-Control",
                                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.data.intent").value("PENALTY_STATUS"))
                .andExpect(jsonPath("$.data.message").value("패널티가 3일 남아 있어요."))
                .andExpect(jsonPath("$.data.facts.penaltyDays").value(3))
                .andExpect(jsonPath("$.data.facts.releaseDate").value("2026-10-10"));
    }

    @Test
    @DisplayName("요청에 다른 사용자 ID 나 이름을 실어도 무시하고 항상 인증된 본인으로 조회한다")
    void ignoresTargetFieldsInBody() throws Exception {
        given(useCase.answer(anyLong(), any()))
                .willReturn(
                        new PersonalAnswer(
                                PersonalIntent.LENT_EXPIRY,
                                "대여 중인 사물함이 없어요.",
                                new PersonalFacts.LentExpiry(false, null, null, null, null)));

        mvc.perform(
                        post("/v4/chatbot/personal")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"intent\":\"LENT_EXPIRY\",\"userId\":1,\"id\":1,"
                                                + "\"name\":\"intra1\",\"intraId\":\"intra1\"}"))
                .andExpect(status().isOk());

        verify(useCase).answer(9L, PersonalIntent.LENT_EXPIRY);
        verify(useCase, never()).answer(eq(1L), any());
    }

    @Test
    @DisplayName("다른 사용자로 로그인하면 그 사용자의 ID 로 조회된다(요청 값과 무관)")
    void followsLoginIdentity() throws Exception {
        loginAs(42L);
        given(useCase.answer(anyLong(), any()))
                .willReturn(
                        new PersonalAnswer(
                                PersonalIntent.LENT_EXPIRY,
                                "x",
                                new PersonalFacts.LentExpiry(false, null, null, null, null)));

        mvc.perform(
                        post("/v4/chatbot/personal")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"intent\":\"LENT_EXPIRY\"}"))
                .andExpect(status().isOk());

        verify(useCase).answer(42L, PersonalIntent.LENT_EXPIRY);
    }

    @Test
    @DisplayName("알 수 없는 인텐트, 빈 값, 소문자는 400 이고 서비스를 부르지 않는다")
    void invalidIntent() throws Exception {
        for (String body :
                List.of(
                        "{\"intent\":\"DROP_TABLE\"}",
                        "{\"intent\":\"\"}",
                        "{\"intent\":null}",
                        "{}",
                        "{\"intent\":\"penalty_status\"}")) {
            mvc.perform(
                            post("/v4/chatbot/personal")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.message")
                                    .value(ErrorCode.CHATBOT_INVALID_INTENT.getMessage()));
        }
        verify(useCase, never()).answer(anyLong(), any());
    }

    @Test
    @DisplayName("인증 정보가 없으면 조회하지 않는다")
    void noPrincipal() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(
                        post("/v4/chatbot/personal")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"intent\":\"LENT_EXPIRY\"}"))
                .andExpect(status().isNotFound());

        verify(useCase, never()).answer(anyLong(), any());
    }

    @Test
    @DisplayName("사용자별 한도를 넘으면 429 이다")
    void rateLimited() throws Exception {
        given(useCase.answer(9L, PersonalIntent.LENT_EXPIRY))
                .willThrow(new ServiceException(ErrorCode.TOO_MANY_REQUESTS));

        mvc.perform(
                        post("/v4/chatbot/personal")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"intent\":\"LENT_EXPIRY\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("칩 목록은 개인 정보 없이 인텐트 이름과 문구만 준다")
    void intents() throws Exception {
        mvc.perform(get("/v4/chatbot/personal/intents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].intent").value("LENT_EXPIRY"))
                .andExpect(jsonPath("$.data[0].label").value("내 사물함 만료일 확인"));
    }

    // ---- 구조로 못 박는 보안 규칙 ----

    @Test
    @DisplayName("요청으로 받을 수 있는 값은 인텐트 이름 하나뿐이다(대상 사용자를 지정하는 필드가 없다)")
    void requestHasOnlyIntent() {
        assertThat(
                        Arrays.stream(PersonalRequest.class.getRecordComponents())
                                .map(c -> c.getName())
                                .toList())
                .containsExactly("intent");
    }

    @Test
    @DisplayName("컨트롤러의 모든 핸들러는 경로·쿼리 변수 없이 본문(인텐트)과 인증 정보만 받는다")
    void handlersTakeOnlyBodyAndPrincipal() {
        for (Method method : PersonalChatbotController.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            for (Parameter p : method.getParameters()) {
                boolean body = p.isAnnotationPresent(RequestBody.class);
                boolean principal = p.isAnnotationPresent(AuthenticationPrincipal.class);
                assertThat(body || principal)
                        .as(
                                "%s 의 파라미터 %s 는 @RequestBody 또는 @AuthenticationPrincipal 이어야 한다",
                                method.getName(), p.getName())
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("두 플래그가 모두 true 일 때만 컨트롤러가 등록된다")
    void registeredOnlyWhenBothFlagsOn() {
        ApplicationContextRunner runner =
                new ApplicationContextRunner()
                        .withUserConfiguration(PersonalChatbotController.class)
                        .withBean(PersonalChatbotUseCase.class, () -> useCase);

        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(PersonalChatbotController.class));
        runner.withPropertyValues("app.chatbot.enabled=true")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(PersonalChatbotController.class));
        runner.withPropertyValues("app.chatbot.personal.enabled=true")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(PersonalChatbotController.class));
        runner.withPropertyValues("app.chatbot.enabled=true", "app.chatbot.personal.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(PersonalChatbotController.class));
    }
}
