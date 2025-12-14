package com.gyeongsan.cabinet.auth.service;

import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.item.repository.ItemRepository;
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
    private final ItemRepository itemRepository;
    private final ItemHistoryRepository itemHistoryRepository;

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
        Item lentItem = itemRepository.findByType(ItemType.LENT)
                .orElse(null);

        if (lentItem != null) {
            ItemHistory ticket = new ItemHistory(LocalDateTime.now(), null, user, lentItem);
            itemHistoryRepository.save(ticket);
            log.info("🎁 [Welcome] 신규 유저 {}님께 웰컴 선물(대여권) 지급 완료!", user.getName());
        } else {
            log.warn("⚠️ [Welcome] 지급 실패: DB에 대여권(LENT) 아이템이 없습니다.");
        }
    }
}