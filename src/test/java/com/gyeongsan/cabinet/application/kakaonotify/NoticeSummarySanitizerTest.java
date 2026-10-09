package com.gyeongsan.cabinet.application.kakaonotify;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeSummarySanitizerTest {

    @Test
    @DisplayName("깨끗한 한 문단은 그대로 통과한다")
    void plainTextPasses() {
        assertThat(NoticeSummarySanitizer.clean("10월 15일 18시까지 사물함을 비워 주세요.", 150))
                .contains("10월 15일 18시까지 사물함을 비워 주세요.");
    }

    @Test
    @DisplayName("URL 과 마크다운 링크는 제거된다 - 요약을 통해 피싱 링크가 새어 나가지 않게")
    void removesLinks() {
        assertThat(NoticeSummarySanitizer.clean("신청은 [여기](https://evil.example/x)에서 하세요", 150))
                .contains("신청은 여기에서 하세요");
        assertThat(NoticeSummarySanitizer.clean("https://evil.example/login 에서 로그인하세요", 150))
                .contains("에서 로그인하세요");
        assertThat(NoticeSummarySanitizer.clean("www.evil.example 접속", 150)).contains("접속");
        assertThat(NoticeSummarySanitizer.clean("evil.com 접속", 150)).contains("접속");
    }

    @Test
    @DisplayName("마크다운 기호, 목록 머리표, 이모지는 제거하고 줄바꿈은 공백 하나로 합친다")
    void removesMarkdownEmojiAndNewlines() {
        String raw = "**중요** 공지\n- 일시: 10/15\n- 대상: 전원 🚨\n\r\n`코드`";
        String cleaned = NoticeSummarySanitizer.clean(raw, 150).orElseThrow();
        assertThat(cleaned).isEqualTo("중요 공지 일시: 10/15 대상: 전원 코드");
        assertThat(cleaned).doesNotContain("\n", "*", "`", "🚨");
    }

    @Test
    @DisplayName("요약기가 접두어를 붙여 돌려줘도 한 번만 남게 앞의 접두어는 지운다")
    void removesEchoedPrefix() {
        assertThat(NoticeSummarySanitizer.clean("[공지] 내일 점검합니다", 150)).contains("내일 점검합니다");
        assertThat(NoticeSummarySanitizer.clean("📢 [공지] 내일 점검합니다", 150)).contains("내일 점검합니다");
        assertThat(NoticeSummarySanitizer.clean("요약: 내일 점검합니다", 150)).contains("내일 점검합니다");
    }

    @Test
    @DisplayName("비었거나 정제 후 비면 빈 값이다")
    void emptyResults() {
        assertThat(NoticeSummarySanitizer.clean(null, 150)).isEmpty();
        assertThat(NoticeSummarySanitizer.clean("   \n ", 150)).isEmpty();
        assertThat(NoticeSummarySanitizer.clean("https://only.link/x 🚨", 150)).isEmpty();
    }

    @Test
    @DisplayName("허용 길이를 넘으면 잘라 쓰지 않고 비워 폴백하게 한다(코드 포인트 기준, 경계 포함)")
    void lengthCap() {
        String exactly = "가".repeat(50);
        assertThat(NoticeSummarySanitizer.clean(exactly, 50)).contains(exactly);
        assertThat(NoticeSummarySanitizer.clean(exactly + "가", 50)).isEmpty();
    }

    @Test
    @DisplayName("시간·날짜 범위의 ~ 는 지우지 않는다 (수치 보존)")
    void keepsTildeInRanges() {
        assertThat(NoticeSummarySanitizer.clean("09:00~18:00", 150)).contains("09:00~18:00");
        assertThat(NoticeSummarySanitizer.clean("10/20(월)~10/24(금)", 150))
                .contains("10/20(월)~10/24(금)");
        assertThat(NoticeSummarySanitizer.clean("운영 시간은 09:00 ~ 18:00 입니다", 150))
                .contains("운영 시간은 09:00 ~ 18:00 입니다");
    }

    @Test
    @DisplayName("본문 중간의 > 는 유지하고, 행 맨 앞의 인용 > 만 제거한다")
    void keepsMidLineGreaterThan() {
        assertThat(NoticeSummarySanitizer.clean("A동 > B동", 150)).contains("A동 > B동");
        assertThat(NoticeSummarySanitizer.clean("> 인용된 안내", 150)).contains("인용된 안내");
        assertThat(NoticeSummarySanitizer.clean("> 첫 줄\n> 둘째 줄", 150)).contains("첫 줄 둘째 줄");
        assertThat(NoticeSummarySanitizer.clean("안내\n>> 중첩 인용\n끝", 150)).contains("안내 중첩 인용 끝");
        assertThat(NoticeSummarySanitizer.clean("1층 > 2층 > 3층", 150)).contains("1층 > 2층 > 3층");
    }

    @Test
    @DisplayName("취소선(연속 ~~)의 표시만 제거하고, 단독 ~ 는 남긴다")
    void removesOnlyStrikethroughMarkers() {
        assertThat(NoticeSummarySanitizer.clean("~~취소~~ 텍스트", 150)).contains("취소 텍스트");
        assertThat(NoticeSummarySanitizer.clean("~~취소~~ 10/20~10/24 진행", 150))
                .contains("취소 10/20~10/24 진행");
    }

    @Test
    @DisplayName("숫자 범위·날짜·시간 표기(10-15, 3/4, 09:00, 1.5)는 건드리지 않는다")
    void keepsNumericNotation() {
        String text = "10-15 3/4 09:00 1.5배 2026.10.15 (월) 5~10명 100%";
        assertThat(NoticeSummarySanitizer.clean(text, 150)).contains(text);
    }

    @Test
    @DisplayName("*, _, `, #, | 같은 마크다운 기호는 기존대로 제거된다")
    void stillRemovesOtherMarkdownChars() {
        assertThat(NoticeSummarySanitizer.clean("*굵게* _기울임_ `코드` # 제목 a|b", 150))
                .contains("굵게 기울임 코드 제목 ab");
    }
}
