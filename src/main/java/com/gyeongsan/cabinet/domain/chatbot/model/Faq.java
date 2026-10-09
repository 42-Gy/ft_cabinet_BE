package com.gyeongsan.cabinet.domain.chatbot.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자주 묻는 질문 하나. 답변은 관리자가 미리 써 둔 문장이고, 같은 답변을 가리키는 질문 표현이 여러 개 있다. 질문 표현이 많을수록 다르게 물어도 찾기 쉽다.
 *
 * @param id 저장 전에는 null
 * @param seedKey 초기 데이터로 들어온 항목의 고정 키. 직접 추가한 항목은 null
 * @param category 화면에서 묶어 보여줄 분류(대여, 반납 등)
 * @param answer 사용자에게 그대로 보여줄 답변
 * @param enabled false 면 검색과 목록에서 제외한다
 * @param questions 이 답변에 대응하는 질문 표현들(첫 번째가 대표 질문)
 */
public record Faq(
        Long id,
        String seedKey,
        String category,
        String answer,
        boolean enabled,
        List<String> questions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public Faq {
        questions = questions == null ? List.of() : List.copyOf(questions);
    }

    public String representativeQuestion() {
        return questions.isEmpty() ? "" : questions.get(0);
    }
}
