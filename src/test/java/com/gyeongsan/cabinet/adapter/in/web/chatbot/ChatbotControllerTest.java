package com.gyeongsan.cabinet.adapter.in.web.chatbot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.application.chatbot.ChatbotSettings;
import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.port.in.AskChatbotUseCase;
import com.gyeongsan.cabinet.domain.chatbot.port.in.FaqAdminUseCase;
import com.gyeongsan.cabinet.domain.chatbot.port.in.FaqQueryUseCase;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.GlobalExceptionHandler;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatbotControllerTest {

    private final AskChatbotUseCase ask = Mockito.mock(AskChatbotUseCase.class);
    private final FaqQueryUseCase query = Mockito.mock(FaqQueryUseCase.class);
    private final FaqAdminUseCase admin = Mockito.mock(FaqAdminUseCase.class);
    private MockMvc mvc;

    private static Faq faq(
            long id, String category, String answer, boolean enabled, String... questions) {
        return new Faq(
                id,
                null,
                category,
                answer,
                enabled,
                List.of(questions),
                LocalDateTime.of(2026, 10, 7, 12, 0),
                LocalDateTime.of(2026, 10, 7, 12, 30));
    }

    @BeforeEach
    void setUp() {
        ObjectMapper[] holder = new ObjectMapper[1];
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(ctx -> holder[0] = ctx.getBean(ObjectMapper.class));
        ChatbotSettings settings = new ChatbotSettings(0.8, 0.6, 3, 200, 2, "못 찾았어요");
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new ChatbotController(ask, query, settings),
                                new AdminFaqController(admin))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(holder[0]))
                        .build();
    }

    @Test
    @DisplayName("찾았으면 MATCHED 와 답변을 준다")
    void askMatched() throws Exception {
        Faq f = faq(1, "대여", "대여 답변", true, "대여 방법", "빌리는 법");
        given(ask.ask("대여 어떻게 해요"))
                .willReturn(
                        new ChatbotAnswer(
                                ChatbotAnswer.Result.MATCHED, f, "대여 방법", 0.91, List.of()));

        mvc.perform(
                        post("/v4/chatbot/ask")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"대여 어떻게 해요\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("MATCHED"))
                .andExpect(jsonPath("$.data.answer.faqId").value(1))
                .andExpect(jsonPath("$.data.answer.question").value("대여 방법"))
                .andExpect(jsonPath("$.data.answer.answer").value("대여 답변"))
                .andExpect(jsonPath("$.data.suggestions").isEmpty())
                // 유사도 점수 같은 내부 값은 노출하지 않는다.
                .andExpect(jsonPath("$.data.score").doesNotExist());
    }

    @Test
    @DisplayName("비슷한 후보만 있으면 SUGGESTED 와 후보 질문 목록을 준다")
    void askSuggested() throws Exception {
        given(ask.ask("애매"))
                .willReturn(
                        new ChatbotAnswer(
                                ChatbotAnswer.Result.SUGGESTED,
                                null,
                                "대여 방법",
                                0.7,
                                List.of(
                                        new ChatbotAnswer.Suggestion(1, "대여 방법", 0.7),
                                        new ChatbotAnswer.Suggestion(2, "반납 방법", 0.65))));

        mvc.perform(
                        post("/v4/chatbot/ask")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"애매\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("SUGGESTED"))
                .andExpect(jsonPath("$.data.answer").doesNotExist())
                .andExpect(jsonPath("$.data.suggestions.length()").value(2))
                .andExpect(jsonPath("$.data.suggestions[1].faqId").value(2));
    }

    @Test
    @DisplayName("못 찾았으면 UNMATCHED 와 설정된 안내 문구를 준다")
    void askUnmatched() throws Exception {
        given(ask.ask("학식")).willReturn(ChatbotAnswer.unmatched(0.1));

        mvc.perform(
                        post("/v4/chatbot/ask")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"학식\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("UNMATCHED"))
                .andExpect(jsonPath("$.data.message").value("못 찾았어요"));
    }

    @Test
    @DisplayName("잘못된 질문은 400, 챗봇을 쓸 수 없으면 503 이다")
    void askErrors() throws Exception {
        given(ask.ask("")).willThrow(new ServiceException(ErrorCode.CHATBOT_INVALID_QUESTION));
        given(ask.ask("대여")).willThrow(new ServiceException(ErrorCode.CHATBOT_NOT_READY));

        mvc.perform(
                        post("/v4/chatbot/ask")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/v4/chatbot/ask")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"대여\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("FAQ 목록은 분류별로 묶고 대표 질문만 보여준다")
    void listGroupedByCategory() throws Exception {
        given(query.listEnabled())
                .willReturn(
                        List.of(
                                faq(1, "대여", "a", true, "대여 방법", "빌리는 법"),
                                faq(2, "반납", "b", true, "반납 방법"),
                                faq(3, "대여", "c", true, "대여권이 없어요")));

        mvc.perform(get("/v4/chatbot/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories.length()").value(2))
                .andExpect(jsonPath("$.data.categories[0].category").value("대여"))
                .andExpect(jsonPath("$.data.categories[0].faqs.length()").value(2))
                .andExpect(jsonPath("$.data.categories[0].faqs[0].question").value("대여 방법"))
                .andExpect(jsonPath("$.data.categories[1].category").value("반납"));
    }

    @Test
    @DisplayName("FAQ 하나를 조회하고, 없거나 숨긴 FAQ 는 404 이다")
    void getOne() throws Exception {
        given(query.getEnabled(1)).willReturn(faq(1, "대여", "대여 답변", true, "대여 방법"));
        given(query.getEnabled(9)).willThrow(new ServiceException(ErrorCode.FAQ_NOT_FOUND));

        mvc.perform(get("/v4/chatbot/faqs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("대여 답변"));
        mvc.perform(get("/v4/chatbot/faqs/9")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("관리자: 추가하면 생성된 FAQ 를 돌려주고, enabled 를 생략하면 true 로 처리한다")
    void adminCreate() throws Exception {
        given(admin.create(eq("대여"), eq("답"), anyBoolean(), anyList()))
                .willReturn(faq(5, "대여", "답", true, "질문"));

        mvc.perform(
                        post("/v4/admin/faqs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"category\":\"대여\",\"answer\":\"답\",\"questions\":[\"질문\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.questions[0]").value("질문"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-10-07T12:00:00"));
        verify(admin).create("대여", "답", true, List.of("질문"));
    }

    @Test
    @DisplayName("관리자: 입력 오류는 400, 없는 FAQ 는 404 이다")
    void adminErrors() throws Exception {
        given(admin.create(anyString(), anyString(), anyBoolean(), any()))
                .willThrow(new ServiceException(ErrorCode.FAQ_INVALID));
        given(admin.update(eq(9L), anyString(), anyString(), anyBoolean(), any()))
                .willThrow(new ServiceException(ErrorCode.FAQ_NOT_FOUND));
        Mockito.doThrow(new ServiceException(ErrorCode.FAQ_NOT_FOUND))
                .when(admin)
                .delete(anyLong());

        mvc.perform(
                        post("/v4/admin/faqs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"category\":\"x\",\"answer\":\"y\",\"questions\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/v4/admin/faqs/9")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"category\":\"x\",\"answer\":\"y\",\"questions\":[\"q\"]}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/v4/admin/faqs/9")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("관리자: 목록, 상세, 삭제")
    void adminListGetDelete() throws Exception {
        given(admin.listAll())
                .willReturn(List.of(faq(1, "대여", "a", true, "q1"), faq(2, "반납", "b", false, "q2")));
        given(admin.get(2)).willReturn(faq(2, "반납", "b", false, "q2"));

        mvc.perform(get("/v4/admin/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].enabled").value(false));
        mvc.perform(get("/v4/admin/faqs/2")).andExpect(jsonPath("$.data.id").value(2));
        mvc.perform(delete("/v4/admin/faqs/1")).andExpect(status().isOk());
        verify(admin).delete(1);
    }
}
