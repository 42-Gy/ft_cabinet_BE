package com.gyeongsan.cabinet.global.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gyeongsan.cabinet.adapter.in.web.chatbot.ChatbotController;
import com.gyeongsan.cabinet.adapter.in.web.chatbot.dto.ChatbotDtos.AskRequest;
import com.gyeongsan.cabinet.adapter.in.web.lent.LentController;
import com.gyeongsan.cabinet.adapter.in.web.lent.dto.LentReturnRequest;
import com.gyeongsan.cabinet.application.chatbot.ChatbotSettings;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.port.in.AskChatbotUseCase;
import com.gyeongsan.cabinet.domain.chatbot.port.in.FaqQueryUseCase;
import com.gyeongsan.cabinet.domain.lent.port.in.LentUseCase;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 컨트롤러 호출 로그에 사용자 입력(챗봇 질문, 반납 공유 비밀번호, 사유 등)이 남지 않는지 실제 컨트롤러를 AOP 프록시로 감싸 확인한다. 이전에는 인자를 그대로 찍어 이런
 * 값이 콘솔과 30일 보관 파일 로그에 남았다.
 */
class LoggingAspectTest {

    private static final String SECRET_QUESTION = "SECRET-QUESTION-내 패널티 언제 풀려요 홍길동";
    private static final String SECRET_PASSWORD = "9137";
    private static final String SECRET_REASON = "SECRET-REASON-사진이 자꾸 실패해서요";

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attach() {
        logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v4/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
        RequestContextHolder.resetRequestAttributes();
    }

    private String allLogs() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
    }

    private static <T> T proxied(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new LoggingAspect());
        return factory.getProxy();
    }

    @Test
    @DisplayName("챗봇 질문 원문은 로그에 남지 않는다")
    void chatbotQuestionIsNotLogged() {
        AskChatbotUseCase ask = q -> ChatbotAnswer.unmatched(0.1);
        ChatbotSettings settings = new ChatbotSettings(0.9, 0.8, 3, 200, 2, "m");
        ChatbotController controller =
                proxied(new ChatbotController(ask, mock(FaqQueryUseCase.class), settings));

        controller.ask(new AskRequest(SECRET_QUESTION));

        String logs = allLogs();
        assertThat(logs).contains("[REQUEST]").contains("/v4/test").contains("AskRequest");
        assertThat(logs).doesNotContain("SECRET-QUESTION").doesNotContain("홍길동");
    }

    @Test
    @DisplayName("반납 요청의 공유 비밀번호, 사유, 파일 내용은 로그에 남지 않는다")
    void returnPasswordIsNotLogged() {
        LentUseCase lentUseCase = mock(LentUseCase.class);
        UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
        User user = mock(User.class);
        when(user.getName()).thenReturn("tester");
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        LentController controller = proxied(new LentController(lentUseCase, userRepository));

        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUserId()).thenReturn(42L);
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "cabinet.jpg", "image/jpeg", "PHOTO-BYTES".getBytes());

        try {
            controller.endLentCabinet(file, SECRET_PASSWORD, false, SECRET_REASON, principal);
        } catch (RuntimeException ignored) {
            // 응답 조립 중 목 객체 때문에 실패해도, 요청 로그는 이미 남은 뒤다.
        }

        String logs = allLogs();
        assertThat(logs).contains("[REQUEST]");
        assertThat(logs)
                .doesNotContain(SECRET_PASSWORD)
                .doesNotContain("SECRET-REASON")
                .doesNotContain("cabinet.jpg")
                .doesNotContain("PHOTO-BYTES");
        // 값은 가렸지만 어떤 타입의 인자가 들어왔는지는 디버깅에 남는다.
        assertThat(logs).contains("MockMultipartFile").contains("false");
    }

    @Test
    @DisplayName("숫자, 불리언, 열거형, 날짜 같은 값만 그대로 남기고 나머지는 타입 이름만 남긴다")
    void summarizeKeepsOnlyNonUserInputTypes() {
        Object[] args = {
            42L,
            7,
            Boolean.TRUE,
            Thread.State.NEW,
            LocalDate.of(2026, 10, 6),
            "free text",
            new AskRequest("질문"),
            new StringBuilder("sb"),
            new ArrayList<>(List.of("a", "b")),
            null
        };

        assertThat(LoggingAspect.summarize(args))
                .isEqualTo(
                        "[42, 7, true, NEW, 2026-10-06, String, AskRequest, StringBuilder, ArrayList, null]");
        assertThat(LoggingAspect.summarize(new Object[0])).isEqualTo("[]");
        assertThat(LoggingAspect.summarize(null)).isEqualTo("[]");
    }

    @Test
    @DisplayName("DTO 가 우연히 로그에 찍혀도 값이 보이지 않는다(toString 마스킹)")
    void dtoToStringIsMasked() {
        assertThat(new AskRequest(SECRET_QUESTION).toString())
                .doesNotContain("SECRET")
                .contains("자");
        assertThat(new AskRequest(null).toString()).contains("0자");
        assertThat(new LentReturnRequest(SECRET_PASSWORD, SECRET_REASON).toString())
                .doesNotContain(SECRET_PASSWORD)
                .doesNotContain("SECRET-REASON")
                .contains("****");
    }
}
