package com.gyeongsan.cabinet.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/** app.frontend.url(FRONTEND_URL) 검증과 origin 추출. 잘못된 값이면 부팅 단계에서 실패시킨다. */
public final class FrontendUrls {

    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    private FrontendUrls() {}

    /** yml 에서 "${FRONTEND_URL}/..." 로 이어 붙이므로 끝의 '/' 는 허용하지 않는다. */
    public static String requireValidBase(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("app.frontend.url(FRONTEND_URL)이 비어 있습니다.");
        }
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            throw new IllegalStateException(
                    "app.frontend.url(FRONTEND_URL) 끝의 '/'를 제거해주세요: " + trimmed);
        }

        URI uri = parse(trimmed);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            throw new IllegalStateException(
                    "app.frontend.url(FRONTEND_URL)은 scheme 과 host 를 포함한 절대 URL 이어야 합니다: "
                            + trimmed);
        }

        boolean https = "https".equalsIgnoreCase(scheme);
        boolean loopbackHttp =
                "http".equalsIgnoreCase(scheme)
                        && LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        if (!https && !loopbackHttp) {
            throw new IllegalStateException(
                    "app.frontend.url(FRONTEND_URL)은 https 여야 합니다(로컬 호스트만 http 허용): " + trimmed);
        }

        if (uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || uri.getRawUserInfo() != null) {
            throw new IllegalStateException(
                    "app.frontend.url(FRONTEND_URL)에는 query, fragment, 사용자 정보를 넣을 수 없습니다: "
                            + trimmed);
        }
        return trimmed;
    }

    /** scheme://host[:port] 형태의 origin. CORS 허용 origin 비교에 쓴다. */
    public static String originOf(String frontendUrl) {
        URI uri = parse(requireValidBase(frontendUrl));
        StringBuilder origin =
                new StringBuilder(uri.getScheme().toLowerCase(Locale.ROOT))
                        .append("://")
                        .append(uri.getHost().toLowerCase(Locale.ROOT));
        if (uri.getPort() != -1) {
            origin.append(':').append(uri.getPort());
        }
        return origin.toString();
    }

    private static URI parse(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(
                    "app.frontend.url(FRONTEND_URL) 형식이 올바르지 않습니다: " + url, e);
        }
    }
}
