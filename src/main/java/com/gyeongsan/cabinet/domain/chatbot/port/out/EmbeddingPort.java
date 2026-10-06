package com.gyeongsan.cabinet.domain.chatbot.port.out;

import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;

/** 문장을 의미 벡터로 바꾼다. 돌려주는 벡터는 길이가 1(L2 정규화)이라 내적이 곧 코사인 유사도다. */
public interface EmbeddingPort {

    /**
     * @throws EmbeddingUnavailableException 모델을 쓸 수 없는 경우
     */
    float[] embed(String text);

    /** 어떤 모델/방식으로 만든 벡터인지(로그와 평가 표시용). */
    String modelId();
}
