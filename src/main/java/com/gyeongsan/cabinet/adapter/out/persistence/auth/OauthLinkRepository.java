package com.gyeongsan.cabinet.adapter.out.persistence.auth;

import com.gyeongsan.cabinet.domain.auth.OauthLink;
import com.gyeongsan.cabinet.domain.user.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OauthLinkRepository extends JpaRepository<OauthLink, Long> {

    Optional<OauthLink> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByUserAndProvider(User user, String provider);

    boolean existsByProviderAndProviderId(String provider, String providerId);
}
