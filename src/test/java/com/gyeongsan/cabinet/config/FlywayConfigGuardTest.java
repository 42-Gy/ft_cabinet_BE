package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

/** CLAUDE.md 절대 규칙 3, 4 를 설정 수준에서 지키는지 확인한다. */
class FlywayConfigGuardTest {

    private static final Path MAIN_YML = Path.of("src/main/resources/application.yml");

    private static StandardEnvironment environment(Map<String, Object> overrides)
            throws IOException {
        StandardEnvironment env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        sources.addFirst(new MapPropertySource("test-values", overrides));
        for (PropertySource<?> source :
                new YamlPropertySourceLoader()
                        .load("main-application-yml", new FileSystemResource(MAIN_YML))) {
            sources.addLast(source);
        }
        return env;
    }

    @Test
    @DisplayName("FLYWAY_ENABLED 를 주지 않으면 Flyway 는 꺼져 있다")
    void flywayIsDisabledByDefault() throws IOException {
        assertThat(environment(Map.of()).getProperty("spring.flyway.enabled", Boolean.class))
                .isFalse();
    }

    @Test
    @DisplayName("FLYWAY_ENABLED=true 로 명시한 환경에서만 켜진다")
    void flywayIsEnabledOnlyWhenExplicit() throws IOException {
        assertThat(
                        environment(Map.of("FLYWAY_ENABLED", "true"))
                                .getProperty("spring.flyway.enabled", Boolean.class))
                .isTrue();
    }

    @Test
    @DisplayName("baselineOnMigrate 는 어떤 경우에도 false 이고 clean 은 막혀 있다")
    void baselineOnMigrateIsNeverEnabled() throws IOException {
        StandardEnvironment env = environment(Map.of("FLYWAY_ENABLED", "true"));

        assertThat(env.getProperty("spring.flyway.baseline-on-migrate", Boolean.class)).isFalse();
        assertThat(env.getProperty("spring.flyway.clean-disabled", Boolean.class)).isTrue();
    }
}
