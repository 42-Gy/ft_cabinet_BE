package com.gyeongsan.cabinet.domain.chatbot.port.out;

import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import java.util.List;
import java.util.Optional;

public interface FaqRepositoryPort {

    List<Faq> findAll();

    List<Faq> findAllEnabled();

    Optional<Faq> findById(long id);

    /** id 가 없으면 새로 만들고, 있으면 수정한다. 저장된 결과(id, 시각 포함)를 돌려준다. */
    Faq save(Faq faq);

    /** 존재하지 않으면 false. */
    boolean deleteById(long id);

    long count();

    /** 내용이 바뀌었는지 가볍게 알아보기 위한 값(개수와 마지막 수정 시각). 여러 서버가 각자 검색 색인을 다시 만들 때 쓴다. */
    String fingerprint();
}
