package com.gyeongsan.cabinet.application.kakaonotify;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 요약기(외부 LLM)의 응답을 카톡에 보내도 안전한 평문으로 정제한다. LLM 출력은 공지 본문(신뢰할 수 없는 입력)의 영향을 받을 수 있으므로, 프롬프트의 지시를 믿지
 * 않고 여기서 다시 막는다: 링크·마크다운·이모지·줄바꿈 제거, 접두어 중복 제거, 길이 상한.
 */
final class NoticeSummarySanitizer {

    private static final Pattern MD_LINK = Pattern.compile("\\[([^\\]]*)\\]\\([^)]*\\)");
    private static final Pattern URL =
            Pattern.compile(
                    "(?i)(?:https?://|www\\.)\\S+"
                            + "|\\b(?:[a-z0-9-]+\\.)+(?:com|net|org|io|kr|me|co|app|dev|xyz|link|site|info|biz)\\b(?:/\\S*)?");
    private static final Pattern MD_CHARS = Pattern.compile("[*_`#>~|]+");
    private static final Pattern BULLET =
            Pattern.compile("(?m)^\\s*(?:[-•·▶▷◆◇■□●○]|\\d+[.)])\\s+");
    private static final Pattern EMOJI =
            Pattern.compile(
                    "[\\x{1F000}-\\x{1FFFF}\\x{2600}-\\x{27BF}\\x{2300}-\\x{23FF}\\x{2B00}-\\x{2BFF}\\x{FE00}-\\x{FE0F}\\x{200D}\\x{20E3}]");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\n]]");
    private static final Pattern LEADING_PREFIX =
            Pattern.compile("^\\s*(?:\\[공지\\]|요약\\s*[:：])\\s*");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private NoticeSummarySanitizer() {}

    /**
     * @param raw 요약기가 돌려준 원문
     * @param maxChars 허용하는 최대 글자 수(코드 포인트). 넘으면 잘라 쓰지 않고 비워서 폴백하게 한다(문장 중간에서 끊긴 요약은 오해를 부른다)
     * @return 정제된 요약문. 비었거나 길이를 넘으면 빈 값
     */
    static Optional<String> clean(String raw, int maxChars) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = MD_LINK.matcher(raw).replaceAll("$1");
        text = URL.matcher(text).replaceAll("");
        text = BULLET.matcher(text).replaceAll("");
        text = MD_CHARS.matcher(text).replaceAll("");
        text = EMOJI.matcher(text).replaceAll("");
        text = CONTROL.matcher(text).replaceAll("");
        text = WHITESPACE.matcher(text).replaceAll(" ").strip();
        text = LEADING_PREFIX.matcher(text).replaceFirst("").strip();

        if (text.isEmpty() || text.codePointCount(0, text.length()) > maxChars) {
            return Optional.empty();
        }
        return Optional.of(text);
    }
}
