package com.gyeongsan.cabinet.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

class ApplicationYmlRedirectUriTest {

    private static final Path MAIN_YML = Path.of("src/main/resources/application.yml");
    private static final List<String> BANNED_HOSTS = List.of("subak.site", "azurewebsites.net");

    private static StandardEnvironment environment(Map<String, Object> overrides)
            throws IOException {
        StandardEnvironment env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();
        // 실행 환경의 시스템 속성·환경변수가 결과에 섞이지 않도록 제거한다.
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);

        Map<String, Object> values = new HashMap<>();
        values.put("FT_CLIENT_ID", "ft-id");
        values.put("FT_CLIENT_SECRET", "ft-secret");
        values.put("KAKAO_CLIENT_ID", "kakao-id");
        values.put("KAKAO_CLIENT_SECRET", "kakao-secret");
        values.put("GOOGLE_CLIENT_ID", "google-id");
        values.put("GOOGLE_CLIENT_SECRET", "google-secret");
        values.putAll(overrides);
        sources.addFirst(new MapPropertySource("test-values", values));

        for (PropertySource<?> source :
                new YamlPropertySourceLoader()
                        .load("main-application-yml", new FileSystemResource(MAIN_YML))) {
            sources.addLast(source);
        }
        return env;
    }

    private static OAuth2ClientProperties bind(StandardEnvironment env) {
        return Binder.get(env)
                .bind("spring.security.oauth2.client", OAuth2ClientProperties.class)
                .get();
    }

    @Test
    @DisplayName("운영 FRONTEND_URL 이면 로그인용 redirect URI 가 외부화 이전 값과 정확히 같다")
    void loginRedirectUris_matchLegacyConstants() throws IOException {
        OAuth2ClientProperties props =
                bind(environment(Map.of("FRONTEND_URL", "https://subak.site")));

        assertEquals(
                "https://subak.site/login/oauth2/code/42",
                props.getRegistration().get("42").getRedirectUri());
        assertEquals(
                "https://subak.site/login/oauth2/code/kakao",
                props.getRegistration().get("kakao").getRedirectUri());
        assertEquals(
                "https://subak.site/login/oauth2/code/google",
                props.getRegistration().get("google").getRedirectUri());
    }

    @Test
    @DisplayName("데모 FRONTEND_URL 이면 로그인용 redirect URI 에 운영 도메인이 섞이지 않는다")
    void loginRedirectUris_followFrontendUrl() throws IOException {
        OAuth2ClientProperties props =
                bind(environment(Map.of("FRONTEND_URL", "https://demo.example.com")));

        assertEquals(
                "https://demo.example.com/login/oauth2/code/42",
                props.getRegistration().get("42").getRedirectUri());
        assertEquals(
                "https://demo.example.com/login/oauth2/code/kakao",
                props.getRegistration().get("kakao").getRedirectUri());
        assertEquals(
                "https://demo.example.com/login/oauth2/code/google",
                props.getRegistration().get("google").getRedirectUri());
    }

    @Test
    @DisplayName("CORS_ALLOWED_ORIGINS 가 없어도 빈 값으로 해석되고, 있으면 그대로 전달된다")
    void corsAllowedOrigins_optional() throws IOException {
        assertEquals(
                "",
                environment(Map.of("FRONTEND_URL", "https://demo.example.com"))
                        .getProperty("app.cors.allowed-origins"));
        assertEquals(
                "https://a.example,https://b.example",
                environment(
                                Map.of(
                                        "FRONTEND_URL",
                                        "https://demo.example.com",
                                        "CORS_ALLOWED_ORIGINS",
                                        "https://a.example,https://b.example"))
                        .getProperty("app.cors.allowed-origins"));
    }

    @Test
    @DisplayName("main 소스·설정에 운영 도메인이 하드코딩되어 있지 않다")
    void mainSources_haveNoHardcodedProductionHosts() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main"))) {
            files.filter(Files::isRegularFile)
                    .filter(
                            path ->
                                    path.toString().endsWith(".java")
                                            || path.toString().endsWith(".yml")
                                            || path.toString().endsWith(".properties"))
                    .forEach(
                            path -> {
                                String text = read(path);
                                for (String banned : BANNED_HOSTS) {
                                    if (text.contains(banned)) {
                                        offenders.add(path + " -> " + banned);
                                    }
                                }
                            });
        }

        assertTrue(offenders.isEmpty(), "운영 식별자가 하드코딩되어 있습니다: " + offenders);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("파일을 읽을 수 없습니다: " + path, e);
        }
    }
}
