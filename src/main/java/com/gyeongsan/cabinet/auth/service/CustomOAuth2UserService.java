package com.gyeongsan.cabinet.auth.service;

import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.domain.UserRole;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> attributes = oAuth2User.getAttributes();

            String intraId = (String) attributes.get("login");
            String email = (String) attributes.get("email");

            // 42 API는 UTC 시간을 ISO 포맷으로 반환합니다.
            String blackholedAtStr = (String) attributes.get("blackholed_at");

            // 👇 [디버깅 로그] 42 API가 보낸 원본 문자열 확인
            log.warn("🚨 RAW Blackholed At String: {}", blackholedAtStr);

            saveOrUpdateUser(intraId, email, blackholedAtStr); // 블랙홀 데이터 전달 및 DB 저장

            return oAuth2User;
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Spring Security 6+ 오류 처리 방식 적용
            OAuth2Error error = new OAuth2Error(
                    "user_info_error",
                    "OAuth2 사용자 정보 로딩 중 오류 발생: " + ex.getMessage(),
                    null
            );
            throw new OAuth2AuthenticationException(error, ex);
        }
    }

    private void saveOrUpdateUser(String intraId, String email, String blackholedAtStr) {
        User user = userRepository.findByName(intraId)
                .orElseGet(() -> {
                    log.info("신규 유저 발견! 회원가입 진행: {}", intraId);
                    return User.of(intraId, email, UserRole.USER);
                });

        // 1. Blackhole 날짜 파싱 및 KST로 변환
        LocalDateTime blackholedAt = null;
        if (blackholedAtStr != null && !blackholedAtStr.isEmpty()) {
            try {
                // ZonedDateTime으로 파싱 (UTC 정보 포함) 후 KST(Asia/Seoul)로 변환하여 LocalTime 저장
                ZonedDateTime utcTime = ZonedDateTime.parse(blackholedAtStr);
                blackholedAt = utcTime.withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime();
            } catch (Exception e) {
                log.error("블랙홀 날짜 파싱 실패 (RAW: {}): {}", blackholedAtStr, e.getMessage());
                // 파싱 실패 시 blackholedAt은 null로 남음
            }
        }

        // 2. [업데이트] DB에 최신 Blackhole 일자 저장
        user.updateBlackholedAt(blackholedAt);

        // 3. (옵션) 기존 출석 체크 및 코인 지급 로직이 있다면 여기에 삽입
        // user.attendance();

        userRepository.save(user);
    }
}