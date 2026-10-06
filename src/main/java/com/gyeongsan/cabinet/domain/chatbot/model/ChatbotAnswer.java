package com.gyeongsan.cabinet.domain.chatbot.model;

import java.util.List;

/**
 * 질문에 대한 챗봇의 결과.
 *
 * @param result 찾음 / 비슷한 후보만 있음 / 못 찾음
 * @param faq 찾은 FAQ(MATCHED 일 때만)
 * @param matchedQuestion 가장 비슷했던 질문 표현
 * @param score 가장 높은 유사도(코사인, -1~1)
 * @param suggestions SUGGESTED 일 때는 비슷한 후보들, MATCHED 일 때는 "혹시 이 질문인가요?" 대안 후보(없을 수 있음)
 */
public record ChatbotAnswer(
        Result result,
        Faq faq,
        String matchedQuestion,
        double score,
        List<Suggestion> suggestions) {

    public enum Result {
        MATCHED,
        SUGGESTED,
        UNMATCHED
    }

    public record Suggestion(long faqId, String question, double score) {}

    public static ChatbotAnswer unmatched(double bestScore) {
        return new ChatbotAnswer(Result.UNMATCHED, null, null, bestScore, List.of());
    }
}
