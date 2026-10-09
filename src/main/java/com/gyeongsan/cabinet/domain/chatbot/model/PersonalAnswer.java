package com.gyeongsan.cabinet.domain.chatbot.model;

/**
 * 개인화 답변. message 는 서버가 고정 문장에 값을 채워 만든다(생성형 모델을 쓰지 않는다).
 *
 * @param facts 같은 내용을 구조화한 값. 화면 표시용이며 인텐트에 따라 타입이 정해져 있다
 */
public record PersonalAnswer(PersonalIntent intent, String message, PersonalFacts facts) {}
