package com.gyeongsan.cabinet.adapter.in.web.chatbot.dto;

import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import java.util.List;

/** 챗봇 사용자 API 의 요청/응답. 답변 본문은 관리자가 쓴 일반 텍스트이니 화면에서는 HTML 로 해석하지 말고 텍스트로 표시한다. */
public final class ChatbotDtos {

    private ChatbotDtos() {}

    /** question 의 길이와 공백 검증은 서비스가 한다(오류 응답 형식을 다른 API 와 맞추기 위해). */
    public record AskRequest(String question) {}

    public record FaqView(Long faqId, String category, String question, String answer) {
        public static FaqView of(Faq faq, String question) {
            return new FaqView(faq.id(), faq.category(), question, faq.answer());
        }
    }

    public record SuggestionView(Long faqId, String question) {}

    /**
     * @param result MATCHED(찾음) | SUGGESTED(비슷한 후보만 있음) | UNMATCHED(못 찾음)
     * @param answer MATCHED 일 때 답변
     * @param suggestions SUGGESTED 일 때 후보 질문들, MATCHED 일 때는 "혹시 이 질문인가요?" 대안(없으면 빈 목록). 고르면 {@code
     *     GET /v4/chatbot/faqs/{id}} 로 답변을 받는다
     * @param message UNMATCHED 일 때 안내 문구
     */
    public record AskResponse(
            String result, FaqView answer, List<SuggestionView> suggestions, String message) {

        public static AskResponse from(ChatbotAnswer answer, String fallbackMessage) {
            return switch (answer.result()) {
                case MATCHED ->
                        new AskResponse(
                                answer.result().name(),
                                FaqView.of(answer.faq(), answer.faq().representativeQuestion()),
                                answer.suggestions().stream()
                                        .map(s -> new SuggestionView(s.faqId(), s.question()))
                                        .toList(),
                                null);
                case SUGGESTED ->
                        new AskResponse(
                                answer.result().name(),
                                null,
                                answer.suggestions().stream()
                                        .map(s -> new SuggestionView(s.faqId(), s.question()))
                                        .toList(),
                                null);
                case UNMATCHED ->
                        new AskResponse(answer.result().name(), null, List.of(), fallbackMessage);
            };
        }
    }

    public record FaqItem(Long faqId, String question) {}

    public record CategoryGroup(String category, List<FaqItem> faqs) {}

    public record FaqListResponse(List<CategoryGroup> categories) {}
}
