package com.gyeongsan.cabinet.application.chatbot;

/**
 * 챗봇 동작 설정.
 *
 * @param matchThreshold 이 유사도 이상이고 1·2위 차이도 {@code matchMargin} 이상이면 "찾았다"로 보고 답변을 그대로 보여준다
 * @param suggestThreshold 이 유사도 이상이면 "비슷한 질문이 이것 아닌가요?" 후보로 보여준다. 이 값 미만은 못 찾은 것
 * @param maxSuggestions 후보로 보여줄 최대 개수
 * @param matchMargin 1위와 2위 유사도의 최소 차이. 임베딩 점수가 좁은 구간에 몰리는 모델(e5)에서는 절대 점수보다 이 차이가 정답을 더 잘 가려내서,
 *     차이가 작으면 점수가 높아도 자동 답변 대신 후보 제안으로 돌린다. 0 이면 쓰지 않는다
 * @param matchAlternatives 자동 답변(MATCHED)에 함께 돌려줄 "혹시 이 질문인가요?" 대안 후보의 최대 개수. 오답일 때 사용자가 바로 고를 수 있게
 *     한다
 * @param maxQuestionLength 질문 최대 길이(글자 수)
 * @param maxConcurrentEmbeddings 동시에 임베딩을 계산할 수 있는 요청 수. CPU 를 챗봇이 독점하지 못하게 한다
 * @param fallbackMessage 못 찾았을 때 보여줄 안내 문구
 */
public record ChatbotSettings(
        double matchThreshold,
        double suggestThreshold,
        int maxSuggestions,
        double matchMargin,
        int matchAlternatives,
        int maxQuestionLength,
        int maxConcurrentEmbeddings,
        String fallbackMessage) {

    public ChatbotSettings {
        if (suggestThreshold <= 0 || suggestThreshold > matchThreshold || matchThreshold > 1) {
            throw new IllegalArgumentException(
                    "챗봇 임계값은 0 < suggest-threshold <= match-threshold <= 1 이어야 합니다.");
        }
        if (matchMargin < 0 || matchMargin >= 1) {
            throw new IllegalArgumentException("챗봇 match-margin 은 0 이상 1 미만이어야 합니다.");
        }
        if (matchAlternatives < 0) {
            throw new IllegalArgumentException("챗봇 match-alternatives 는 0 이상이어야 합니다.");
        }
        if (maxSuggestions < 0 || maxQuestionLength <= 0 || maxConcurrentEmbeddings <= 0) {
            throw new IllegalArgumentException("챗봇 한도 설정은 양수여야 합니다.");
        }
        if (fallbackMessage == null || fallbackMessage.isBlank()) {
            throw new IllegalArgumentException("챗봇 안내 문구(fallback-message)가 필요합니다.");
        }
    }

    /** margin 과 대안 후보를 쓰지 않는 설정(점수 임계값만으로 판단). */
    public ChatbotSettings(
            double matchThreshold,
            double suggestThreshold,
            int maxSuggestions,
            int maxQuestionLength,
            int maxConcurrentEmbeddings,
            String fallbackMessage) {
        this(
                matchThreshold,
                suggestThreshold,
                maxSuggestions,
                0,
                0,
                maxQuestionLength,
                maxConcurrentEmbeddings,
                fallbackMessage);
    }
}
