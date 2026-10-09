package com.gyeongsan.cabinet.domain.chatbot.port.in;

import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import java.util.List;

/** 사용자 화면용 조회. 사용 중인(enabled) FAQ 만 보인다. */
public interface FaqQueryUseCase {

    List<Faq> listEnabled();

    Faq getEnabled(long id);
}
