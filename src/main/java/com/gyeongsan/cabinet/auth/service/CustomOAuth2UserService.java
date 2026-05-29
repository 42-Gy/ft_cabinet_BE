package com.gyeongsan.cabinet.auth.service;

import com.gyeongsan.cabinet.domain.auth.OauthLink;
import com.gyeongsan.cabinet.domain.auth.port.out.OauthLinkRepositoryPort;
import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.item.repository.ItemRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.domain.UserRole;
import com.gyeongsan.cabinet.user.repository.BannedUserRepository;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Value("${auth.allowed-email-domain}")
    private String allowedEmailDomain;

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemHistoryRepository itemHistoryRepository;
    private final BannedUserRepository bannedUserRepository;
    private final OauthLinkRepositoryPort oauthLinkRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if ("kakao".equals(registrationId) || "google".equals(registrationId)) {
            return handleSocialLogin(userRequest, attributes, registrationId);
        }

        String intraId = (String) attributes.get("login");
        String email = (String) attributes.get("email");

        if (email == null || !email.endsWith(allowedEmailDomain)) {
            log.warn("🚫 비경산 유저 로그인 시도 차단: {} ({})", intraId, email);
            String msg = "42경산 캠퍼스 유저만 사용할 수 있습니다.";
            throw new OAuth2AuthenticationException(new OAuth2Error(msg), msg);
        }

        if (bannedUserRepository.existsByIntraId(intraId)) {
            log.warn("🚫 벤 유저 로그인 시도 차단: {}", intraId);
            String msg = "서비스 이용이 제한된 유저입니다. 관리자에게 문의하세요.";
            throw new OAuth2AuthenticationException(new OAuth2Error(msg), msg);
        }

        LocalDateTime blackholedAt = extractBlackholedAt(attributes);

        saveOrUpdateUser(intraId, email, blackholedAt);

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("USER")),
                attributes,
                userNameAttributeName);
    }

    OAuth2User handleSocialLogin(OAuth2UserRequest userRequest,
                                 Map<String, Object> attributes,
                                 String provider) {
        Object attrValue = "google".equals(provider) ? attributes.get("sub") : attributes.get("id");
        if (attrValue == null) {
            String msg = provider + " 고유 식별자를 가져올 수 없습니다.";
            throw new OAuth2AuthenticationException(new OAuth2Error(msg), msg);
        }
        String providerId = String.valueOf(attrValue);

        OauthLink link = oauthLinkRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> {
                    String msg = "먼저 42 인트라로 로그인하여 " + provider + " 계정을 연동해 주세요.";
                    return new OAuth2AuthenticationException(new OAuth2Error(msg), msg);
                });

        User user = link.getUser();

        if (bannedUserRepository.existsByIntraId(user.getName())) {
            log.warn("🚫 벤 유저 {} 로그인 시도 차단: {}", provider, user.getName());
            String msg = "서비스 이용이 제한된 유저입니다. 관리자에게 문의하세요.";
            throw new OAuth2AuthenticationException(new OAuth2Error(msg), msg);
        }

        Map<String, Object> modifiedAttributes = new HashMap<>(attributes);
        modifiedAttributes.put("login", user.getName());

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        log.info("✅ {} 로그인 성공 (연동 유저): providerId={}, intraId={}", provider, providerId, user.getName());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("USER")),
                modifiedAttributes,
                userNameAttributeName);
    }

    private LocalDateTime extractBlackholedAt(Map<String, Object> attributes) {
        try {
            List<Map<String, Object>> cursusUsers = (List<Map<String, Object>>) attributes.get("cursus_users");

            if (cursusUsers != null) {
                for (Map<String, Object> cursusUser : cursusUsers) {
                    Map<String, Object> cursus = (Map<String, Object>) cursusUser.get("cursus");
                    Integer cursusId = (Integer) cursus.get("id");

                    if (cursusId != null && cursusId == 21) {
                        String dateString = (String) cursusUser.get("blackholed_at");

                        if (dateString != null && !dateString.isEmpty()) {
                            ZonedDateTime utcTime = ZonedDateTime.parse(dateString);
                            return utcTime
                                     .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                                     .toLocalDateTime();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 블랙홀 날짜 파싱 중 오류: {}", e.getMessage());
        }
        return null;
    }

    private void saveOrUpdateUser(String intraId, String email, LocalDateTime blackholedAt) {
        User user = userRepository.findByName(intraId).orElse(null);

        if (user == null) {
            log.info("🎉 신규 유저 발견! 회원가입: {}", intraId);
            user = User.of(intraId, email, UserRole.USER);
            user = userRepository.save(user);
            giveWelcomeGift(user);
        }

        user.updateBlackholedAt(blackholedAt);
        userRepository.save(user);

        log.info("✅ 유저 로그인 처리 완료: {} (블랙홀: {})", intraId, blackholedAt);
    }

    private void giveWelcomeGift(User user) {
        List<ItemType> promoItems = List.of(ItemType.LENT);
        List<ItemHistory> tickets = new java.util.ArrayList<>();

        for (ItemType type : promoItems) {
            itemRepository.findByType(type)
                    .ifPresentOrElse(item -> {
                        tickets.add(new ItemHistory(LocalDateTime.now(), null, user, item));
                    }, () -> log.warn("⚠️ [Beta] 지급 실패: DB에 {} 타입 아이템이 없습니다.", type));
        }

        itemHistoryRepository.saveAll(tickets);
        log.info("🎁 신규 유저 {}님께 웰컴 아이템 {}개 지급 완료", user.getName(), tickets.size());
    }
}