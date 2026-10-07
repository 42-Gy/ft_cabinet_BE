package com.gyeongsan.cabinet.application.kakaonotify;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 슬랙 mrkdwn 원문을 카카오톡 평문으로 바꾼다. 슬랙의 {@code <@U123>}, {@code <https://x|이름>}, {@code <!channel>} 같은
 * 표기는 카카오톡에서는 그대로 보이므로 읽을 수 있는 글자로 풀어 준다. 멘션 대상의 이름은 조회하지 않는다(users.info 를 부르지 않는다).
 */
final class SlackMrkdwn {

    private static final Pattern ANGLE = Pattern.compile("<([^<>]+)>");

    private SlackMrkdwn() {}

    static String toPlainText(String raw) {
        if (raw == null) {
            return "";
        }
        Matcher m = ANGLE.matcher(raw);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(convert(m.group(1))));
        }
        m.appendTail(sb);

        String text =
                sb.toString()
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&amp;", "&")
                        .replace("\r\n", "\n");
        return text.replaceAll("\n{3,}", "\n\n").strip();
    }

    private static String convert(String inner) {
        String target = inner;
        String label = null;
        int bar = inner.indexOf('|');
        if (bar >= 0) {
            target = inner.substring(0, bar);
            label = inner.substring(bar + 1);
        }

        if (target.startsWith("!")) { // 특수 멘션·날짜
            String name = target.substring(1);
            if (name.equals("channel") || name.equals("here") || name.equals("everyone")) {
                return "@" + name;
            }
            if (name.startsWith("subteam^")) {
                return hasText(label) ? label : "@그룹";
            }
            return hasText(label) ? label : "";
        }
        if (target.startsWith("@")) { // 사용자 멘션
            return hasText(label) ? "@" + label.replaceFirst("^@", "") : "@멤버";
        }
        if (target.startsWith("#")) { // 채널 링크
            return hasText(label) ? "#" + label : "#채널";
        }
        if (target.startsWith("mailto:")) {
            return hasText(label) ? label : target.substring("mailto:".length());
        }
        // 일반 링크: 이름이 주소와 다르면 "이름(주소)"로 둬서 주소를 잃지 않게 한다.
        if (hasText(label) && !label.equals(target)) {
            return label + "(" + target + ")";
        }
        return target;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /** 코드 포인트 기준으로 자르고, 잘렸으면 끝에 말줄임표를 붙인다(결과는 maxLength 이하). */
    static String truncate(String text, int maxLength) {
        if (text.codePointCount(0, text.length()) <= maxLength) {
            return text;
        }
        int end = text.offsetByCodePoints(0, maxLength - 1);
        return text.substring(0, end) + "…";
    }
}
