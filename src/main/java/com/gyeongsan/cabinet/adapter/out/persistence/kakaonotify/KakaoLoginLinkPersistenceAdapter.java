package com.gyeongsan.cabinet.adapter.out.persistence.kakaonotify;

import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoLoginLinkPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 로그인용 OauthLink 를 읽기만 한다. OauthLink 와 그 저장소 코드는 건드리지 않으려고 이 어댑터가 직접 조회한다(쓰기 없음). */
@Component
public class KakaoLoginLinkPersistenceAdapter implements KakaoLoginLinkPort {

    @PersistenceContext private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findKakaoProviderId(Long userId) {
        List<String> ids =
                entityManager
                        .createQuery(
                                "SELECT l.providerId FROM OauthLink l "
                                        + "WHERE l.user.id = :userId AND l.provider = 'kakao'",
                                String.class)
                        .setParameter("userId", userId)
                        .setMaxResults(1)
                        .getResultList();
        return ids.stream().findFirst();
    }
}
