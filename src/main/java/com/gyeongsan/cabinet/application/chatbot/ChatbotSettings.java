package com.gyeongsan.cabinet.application.chatbot;

/**
 * 챗봇 동작 설정.
 *
 * @param matchThreshold 이 유사도 이상이면 "찾았다"로 보고 답변을 그대로 보여준다
 * @param suggestThreshold 이 유사도 이상이면 "비슷한 질문이 이것 아닌가요?" 후보로 보여준다. 이 값 미만은 못 찾은 것
 * @param maxSuggestions 후보로 보여줄 최대 개수
 * @param maxQuestionLength 질문 최대 길이(글자 수)
 * @param maxConcurrentEmbeddings 동시에 임베딩을 계산할 수 있는 요청 수. CPU 를 챗봇이 독점하지 못하게 한다
 * @param fallbackMessage 못 찾았을 때 보여줄 안내 문구
 */
public record ChatbotSettings(
        double matchThreshold,
        double suggestThreshold,
        int maxSuggestions,
        int maxQuestionLength,
        int maxConcurrentEmbeddings,
        String fallbackMessage) {

    public ChatbotSettings {
        if (suggestThreshold <= 0 || suggestThreshold > matchThreshold || matchThreshold > 1) {
            throw new IllegalArgumentException(
                    "챗봇 임계값은 0 < suggest-threshold <= match-threshold <= 1 이어야 합니다.");
        }
        if (maxSuggestions < 0 || maxQuestionLength <= 0 || maxConcurrentEmbeddings <= 0) {
            throw new IllegalArgumentException("챗봇 한도 설정은 양수여야 합니다.");
        }
        if (fallbackMessage == null || fallbackMessage.isBlank()) {
            throw new IllegalArgumentException("챗봇 안내 문구(fallback-message)가 필요합니다.");
        }
    }
}
