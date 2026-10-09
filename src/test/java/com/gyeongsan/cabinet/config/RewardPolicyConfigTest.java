package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.domain.user.model.LentTicketRewardPolicy;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

class RewardPolicyConfigTest {

    private static final Path MAIN_YML = Path.of("src/main/resources/application.yml");

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(RewardPolicyConfig.class)
                .withInitializer(
                        context -> {
                            try {
                                for (PropertySource<?> source :
                                        new YamlPropertySourceLoader()
                                                .load(
                                                        "main-application-yml",
                                                        new FileSystemResource(MAIN_YML))) {
                                    context.getEnvironment().getPropertySources().addLast(source);
                                }
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
    }

    @Test
    @DisplayName("운영 application.yml 기본값은 일반 4800분, 트센 900분이다")
    void defaultsFromRealYml() {
        runner().run(
                        context -> {
                            LentTicketRewardPolicy policy =
                                    context.getBean(LentTicketRewardPolicy.class);
                            assertThat(policy.defaultThresholdMinutes()).isEqualTo(4800);
                            assertThat(policy.transcenderThresholdMinutes()).isEqualTo(900);
                        });
    }

    @Test
    @DisplayName("환경변수로 기준을 바꿀 수 있다")
    void overridable() {
        runner().withPropertyValues(
                        "LENT_TICKET_THRESHOLD_MINUTES=6000",
                        "LENT_TICKET_TRANSCENDER_THRESHOLD_MINUTES=1200")
                .run(
                        context -> {
                            LentTicketRewardPolicy policy =
                                    context.getBean(LentTicketRewardPolicy.class);
                            assertThat(policy.defaultThresholdMinutes()).isEqualTo(6000);
                            assertThat(policy.transcenderThresholdMinutes()).isEqualTo(1200);
                        });
    }

    @Test
    @DisplayName("잘못된 설정(0, 트센 기준 > 일반 기준)이면 부팅이 실패한다")
    void invalidConfigurationFailsStartup() {
        for (Map<String, String> bad :
                java.util.List.of(
                        Map.of("LENT_TICKET_TRANSCENDER_THRESHOLD_MINUTES", "0"),
                        Map.of("LENT_TICKET_THRESHOLD_MINUTES", "0"),
                        Map.of("LENT_TICKET_TRANSCENDER_THRESHOLD_MINUTES", "9000"))) {
            runner().withPropertyValues(
                            bad.entrySet().stream()
                                    .map(e -> e.getKey() + "=" + e.getValue())
                                    .toArray(String[]::new))
                    .run(context -> assertThat(context).hasFailed());
        }
    }
}
