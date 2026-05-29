package com.gyeongsan.cabinet.adapter.out.persistence.auth;

import com.gyeongsan.cabinet.domain.auth.OauthLink;
import com.gyeongsan.cabinet.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OauthLinkRepository extends JpaRepository<OauthLink, Long> {

    Optional<OauthLink> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByUserAndProvider(User user, String provider);

    boolean existsByProviderAndProviderId(String provider, String providerId);
}
