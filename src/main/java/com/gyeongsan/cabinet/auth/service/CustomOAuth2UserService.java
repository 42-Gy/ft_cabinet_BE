package com.gyeongsan.cabinet.auth.service;

import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.domain.UserRole;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String intraId = (String) attributes.get("login");
        String email = (String) attributes.get("email");

        LocalDateTime blackholedAt = extractBlackholedAt(attributes);

        saveOrUpdateUser(intraId, email, blackholedAt);

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("USER")),
                attributes,
                userNameAttributeName
        );
    }

    private LocalDateTime extractBlackholedAt(Map<String, Object> attributes) {
        try {
            List<Map<String, Object>> cursusUsers =
                    (List<Map<String, Object>>) attributes.get("cursus_users");

            if (cursusUsers != null) {
                for (Map<String, Object> cursusUser : cursusUsers) {
                    Map<String, Object> cursus =
                            (Map<String, Object>) cursusUser.get("cursus");
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
        User user = userRepository.findByName(intraId)
                .orElseGet(() -> {
                    log.info("🎉 신규 유저 발견! 회원가입: {}", intraId);
                    return User.of(intraId, email, UserRole.USER);
                });

        user.updateBlackholedAt(blackholedAt);

        userRepository.save(user);
        log.info("✅ 유저 정보 업데이트 완료: {} (블랙홀: {})", intraId, blackholedAt);
    }
}
