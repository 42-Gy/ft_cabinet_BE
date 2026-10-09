package com.gyeongsan.cabinet.domain.chatbot.port.in;

import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import java.util.List;

public interface FaqAdminUseCase {

    List<Faq> listAll();

    Faq get(long id);

    Faq create(String category, String answer, boolean enabled, List<String> questions);

    Faq update(long id, String category, String answer, boolean enabled, List<String> questions);

    void delete(long id);
}
