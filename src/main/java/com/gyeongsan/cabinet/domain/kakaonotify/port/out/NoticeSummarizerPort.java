package com.gyeongsan.cabinet.domain.kakaonotify.port.out;

import java.util.Optional;

/**
 * 긴 공지 본문을 짧게 요약하는 외부 시스템(LLM 등) 포트.
 *
 * <p>구현체는 본문을 <b>신뢰할 수 없는 데이터</b>로 다뤄야 하며, 요약을 못 만들면(꺼짐, 시간 초과, 오류, 빈 응답, 차단) 예외나 {@link
 * Optional#empty()} 로 알린다. 호출한 쪽은 어떤 경우에도 기존 동작(자르기)으로 폴백한다. 반환값도 신뢰하지 않고 호출한 쪽이 다시 정제·길이 검증한다.
 */
public interface NoticeSummarizerPort {

    /**
     * @param text 요약할 평문 본문(접두어·첨부 안내 제외)
     * @param maxChars 요약문 목표 최대 글자 수(코드 포인트 기준). 프롬프트에 전달하는 힌트이며 강제는 호출한 쪽이 한다
     * @return 요약문. 만들지 못했으면 빈 값
     */
    Optional<String> summarize(String text, int maxChars);

    /** 요약을 쓰지 않는 구현. 기능이 꺼져 있을 때 쓴다. */
    static NoticeSummarizerPort disabled() {
        return (text, maxChars) -> Optional.empty();
    }
}
