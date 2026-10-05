package com.gyeongsan.cabinet.adapter.out.config;

import com.gyeongsan.cabinet.config.FrontendUrls;
import com.gyeongsan.cabinet.domain.auth.port.out.LinkRedirectUriPort;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** 계정 연동용 redirect URI 는 프론트가 콜백을 받는 경로이므로 FRONTEND_URL 에서 파생한다. */
@Component
public class FrontendLinkRedirectUriAdapter implements LinkRedirectUriPort {

    private static final String LINK_CALLBACK_PATH = "/auth/link/callback/{provider}";
    private static final Pattern PROVIDER_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{0,31}$");

    private final String frontendBaseUrl;

    public FrontendLinkRedirectUriAdapter(@Value("${app.frontend.url}") String frontendUrl) {
        this.frontendBaseUrl = FrontendUrls.requireValidBase(frontendUrl);
    }

    @Override
    public String getLinkRedirectUri(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!PROVIDER_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 공급자입니다: " + provider);
        }
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path(LINK_CALLBACK_PATH)
                .buildAndExpand(normalized)
                .toUriString();
    }
}
