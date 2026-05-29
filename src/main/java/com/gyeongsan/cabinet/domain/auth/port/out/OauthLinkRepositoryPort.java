package com.gyeongsan.cabinet.domain.auth.port.out;

import com.gyeongsan.cabinet.domain.auth.OauthLink;
import com.gyeongsan.cabinet.user.domain.User;

import java.util.Optional;

public interface OauthLinkRepositoryPort {

    Optional<OauthLink> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByUserAndProvider(User user, String provider);

    boolean existsByProviderAndProviderId(String provider, String providerId);

    OauthLink save(OauthLink oauthLink);
}
