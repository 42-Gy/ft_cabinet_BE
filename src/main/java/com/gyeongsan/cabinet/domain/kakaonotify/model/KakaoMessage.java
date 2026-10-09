package com.gyeongsan.cabinet.domain.kakaonotify.model;

/**
 * 카카오 "나에게 보내기" 텍스트 메시지.
 *
 * @param text 본문(카카오 제한 200자)
 * @param linkUrl 메시지를 누르면 열리는 주소(카카오 앱에 등록된 도메인이어야 한다)
 */
public record KakaoMessage(String text, String linkUrl) {

    public static final int MAX_TEXT_LENGTH = 200;
}
