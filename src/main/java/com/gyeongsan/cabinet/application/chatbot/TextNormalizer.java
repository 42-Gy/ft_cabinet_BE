package com.gyeongsan.cabinet.application.chatbot;

import java.text.Normalizer;

/** 질문과 FAQ 표현을 같은 방식으로 정리한다. 한글은 맥(자모 분리)과 윈도우(완성형) 입력이 달라서 NFC 로 통일해야 같은 글자로 취급된다. */
public final class TextNormalizer {

    private TextNormalizer() {}

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC).strip().replaceAll("\\s+", " ");
    }
}
