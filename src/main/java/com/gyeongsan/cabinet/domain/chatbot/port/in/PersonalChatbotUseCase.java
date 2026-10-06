package com.gyeongsan.cabinet.domain.chatbot.port.in;

import com.gyeongsan.cabinet.domain.chatbot.model.PersonalAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;

public interface PersonalChatbotUseCase {

    /** 로그인한 사용자 본인의 정보로 답한다. 대상 사용자는 인증 정보에서 얻은 userId 하나뿐이며, 읽기만 하고 아무것도 바꾸지 않는다. */
    PersonalAnswer answer(Long userId, PersonalIntent intent);
}
