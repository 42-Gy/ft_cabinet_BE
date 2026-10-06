package com.gyeongsan.cabinet.adapter.in.web.chatbot.dto;

import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import java.util.List;

/** 챗봇 사용자 API 의 요청/응답. 답변 본문은 관리자가 쓴 일반 텍스트이니 화면에서는 HTML 로 해석하지 말고 텍스트로 표시한다. */
public final class ChatbotDtos {

    private ChatbotDtos() {}

    /** question 의 길이와 공백 검증은 서비스가 한다(오류 응답 형식을 다른 API 와 맞추기 위해). */
    public record AskRequest(String question) {

        /** 질문 원문은 로그에 남기지 않는다(우연히 이 객체가 로그에 찍혀도 길이만 보인다). */
        @Override
        public String toString() {
            return "AskRequest[question=<" + (question == null ? 0 : question.length()) + "자>]";
        }
    }

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
     * @param personalAction "내 정보" 질문처럼 보일 때 제안하는 칩(없으면 null). 이 응답에는 개인 정보가 없고, 칩을 누르면 {@code POST
     *     /v4/chatbot/personal} 로 본인 정보를 조회한다
     */
    public record AskResponse(
            String result,
            FaqView answer,
            List<SuggestionView> suggestions,
            String message,
            PersonalActionView personalAction) {

        public AskResponse(
                String result, FaqView answer, List<SuggestionView> suggestions, String message) {
            this(result, answer, suggestions, message, null);
        }

        public static AskResponse from(ChatbotAnswer answer, String fallbackMessage) {
            AskResponse base = baseFrom(answer, fallbackMessage);
            return answer.personalAction() == null
                    ? base
                    : new AskResponse(
                            base.result(),
                            base.answer(),
                            base.suggestions(),
                            base.message(),
                            PersonalActionView.of(answer.personalAction()));
        }

        private static AskResponse baseFrom(ChatbotAnswer answer, String fallbackMessage) {
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

    /** 화면에 칩으로 보여 줄 개인화 동작. intent 값을 그대로 {@code /v4/chatbot/personal} 에 보낸다. */
    public record PersonalActionView(String intent, String label) {
        public static PersonalActionView of(PersonalIntent intent) {
            return new PersonalActionView(intent.name(), intent.label());
        }
    }

    /** 개인화 조회 요청. 보낼 수 있는 것은 인텐트 이름뿐이다(대상 사용자는 인증 정보로만 정해진다). 값 검증은 서비스 쪽 오류 형식을 맞추려고 컨트롤러가 한다. */
    public record PersonalRequest(String intent) {}

    /** 개인화 답변. facts 는 인텐트별 화이트리스트 레코드이며 message 는 서버의 고정 문장이다. 본인 정보이므로 응답을 캐시하지 않는다. */
    public record PersonalResponse(String intent, String message, PersonalFacts facts) {
        public static PersonalResponse of(PersonalAnswer answer) {
            return new PersonalResponse(answer.intent().name(), answer.message(), answer.facts());
        }
    }

    public record PersonalIntentItem(String intent, String label) {}

    public record FaqItem(Long faqId, String question) {}

    public record CategoryGroup(String category, List<FaqItem> faqs) {}

    public record FaqListResponse(List<CategoryGroup> categories) {}
}
