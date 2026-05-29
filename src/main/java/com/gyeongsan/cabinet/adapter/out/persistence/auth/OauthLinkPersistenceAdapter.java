package com.gyeongsan.cabinet.adapter.out.persistence.auth;

import com.gyeongsan.cabinet.domain.auth.OauthLink;
import com.gyeongsan.cabinet.domain.auth.port.out.OauthLinkRepositoryPort;
import com.gyeongsan.cabinet.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OauthLinkPersistenceAdapter implements OauthLinkRepositoryPort {

    private final OauthLinkRepository oauthLinkRepository;

    @Override
    public Optional<OauthLink> findByProviderAndProviderId(String provider, String providerId) {
        return oauthLinkRepository.findByProviderAndProviderId(provider, providerId);
    }

    @Override
    public boolean existsByUserAndProvider(User user, String provider) {
        return oauthLinkRepository.existsByUserAndProvider(user, provider);
    }

    @Override
    public boolean existsByProviderAndProviderId(String provider, String providerId) {
        return oauthLinkRepository.existsByProviderAndProviderId(provider, providerId);
    }

    @Override
    public OauthLink save(OauthLink oauthLink) {
        return oauthLinkRepository.save(oauthLink);
    }
}
