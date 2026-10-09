package com.gyeongsan.cabinet.domain.chatbot.port.in;

import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;

public interface AskChatbotUseCase {

    ChatbotAnswer ask(String question);
}
