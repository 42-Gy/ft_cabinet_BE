package com.gyeongsan.cabinet.domain.auth.service;

import com.gyeongsan.cabinet.domain.auth.OauthLink;
import com.gyeongsan.cabinet.domain.auth.dto.OAuthUserInfo;
import com.gyeongsan.cabinet.domain.auth.port.in.LinkAccountUseCase;
import com.gyeongsan.cabinet.domain.auth.port.out.OAuthApiClientPort;
import com.gyeongsan.cabinet.domain.auth.port.out.OauthLinkRepositoryPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class OauthLinkService implements LinkAccountUseCase {

    private final OauthLinkRepositoryPort oauthLinkRepository;
    private final UserRepositoryPort userRepository;
    private final List<OAuthApiClientPort> apiClients;

    @Override
    @Transactional
    public void linkAccount(Long userId, String provider, String authorizationCode) {
        if (userId == null) {
            throw new IllegalArgumentException("유저 ID는 필수입니다.");
        }
        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("소셜 로그인 공급자명은 필수입니다.");
        }
        if (authorizationCode == null || authorizationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("인증 코드는 필수입니다.");
        }

        OAuthApiClientPort apiClient = apiClients.stream()
                .filter(client -> client.supports(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 소셜 로그인 공급자입니다: " + provider));

        OAuthUserInfo oauthInfo = apiClient.getOAuthUserInfo(authorizationCode);

        if (oauthInfo == null || oauthInfo.getProviderId() == null || oauthInfo.getProviderId().trim().isEmpty()) {
            throw new IllegalArgumentException("소셜 로그인 사용자 정보가 올바르지 않습니다.");
        }

        if (oauthLinkRepository.existsByProviderAndProviderId(provider.toLowerCase(), oauthInfo.getProviderId())) {
            throw new ServiceException(ErrorCode.OAUTH_ALREADY_LINKED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (oauthLinkRepository.existsByUserAndProvider(user, provider.toLowerCase())) {
            throw new ServiceException(ErrorCode.OAUTH_ALREADY_LINKED_BY_USER);
        }

        OauthLink link = OauthLink.builder()
                .user(user)
                .provider(provider.toLowerCase())
                .providerId(oauthInfo.getProviderId())
                .providerEmail(oauthInfo.getEmail())
                .linkedAt(LocalDateTime.now())
                .build();
        oauthLinkRepository.save(link);

        log.info("🔗 소셜 연동 완료: userId={}, provider={}, providerId={}", userId, provider.toLowerCase(), oauthInfo.getProviderId());
    }
}
