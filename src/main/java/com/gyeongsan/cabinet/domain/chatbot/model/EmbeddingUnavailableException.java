package com.gyeongsan.cabinet.domain.chatbot.model;

/** 임베딩 모델을 쓸 수 없는 상태(모델 파일 없음, 로드 실패 등). 챗봇만 일시적으로 쓸 수 없고 서버의 다른 기능에는 영향이 없다. */
public class EmbeddingUnavailableException extends RuntimeException {

    public EmbeddingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmbeddingUnavailableException(String message) {
        super(message);
    }
}
