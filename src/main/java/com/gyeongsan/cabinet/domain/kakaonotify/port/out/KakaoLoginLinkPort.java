package com.gyeongsan.cabinet.domain.kakaonotify.port.out;

import java.util.Optional;

/** 유저가 로그인용으로 연동해 둔 카카오 계정(OauthLink)을 읽기 전용으로 확인한다. */
public interface KakaoLoginLinkPort {

    /** 이 유저에 연동된 카카오 회원 번호. 연동이 없으면 비어 있다. */
    Optional<String> findKakaoProviderId(Long userId);
}
