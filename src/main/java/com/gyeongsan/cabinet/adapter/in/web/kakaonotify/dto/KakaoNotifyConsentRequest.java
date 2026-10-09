package com.gyeongsan.cabinet.adapter.in.web.kakaonotify.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/** 인가 코드는 한 번 쓰면 끝이지만 그래도 로그에 남지 않도록 toString 을 만들지 않는다(@Getter 만 사용). */
@Getter
@NoArgsConstructor
public class KakaoNotifyConsentRequest {
    private String authorizationCode;
}
