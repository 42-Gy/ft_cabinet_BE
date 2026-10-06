package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.gyeongsan.cabinet.application.chatbot.PerUserRateLimiter;
import com.gyeongsan.cabinet.application.chatbot.PersonalChatbotService;
import com.gyeongsan.cabinet.application.chatbot.PersonalIntentRouter;
import com.gyeongsan.cabinet.domain.chatbot.port.out.FaqRepositoryPort;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.LentTicketRewardPolicy;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

/** 개인화 기능의 켜짐 조건과 빈 구성을 운영 application.yml 기본값으로 확인한다. */
class ChatbotPersonalWiringTest {

    private static final Path MAIN_YML = Path.of("src/main/resources/application.yml");

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(ChatbotConfig.class, ChatbotPersonalConfig.class)
                .withBean(FaqRepositoryPort.class, () -> mock(FaqRepositoryPort.class))
                .withBean(UserRepositoryPort.class, () -> mock(UserRepositoryPort.class))
                .withBean(LentRepositoryPort.class, () -> mock(LentRepositoryPort.class))
                .withBean(
                        ItemHistoryRepositoryPort.class,
                        () -> mock(ItemHistoryRepositoryPort.class))
                .withBean(LentTicketRewardPolicy.class, () -> new LentTicketRewardPolicy(4800, 900))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
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
    @DisplayName("운영 기본값에서는 챗봇을 켜도 개인화는 꺼져 있다")
    void personalOffByDefault() {
        runner().withPropertyValues("CHATBOT_ENABLED=true", "CHATBOT_EMBEDDING_PROVIDER=ngram")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).doesNotHaveBean(PersonalChatbotService.class);
                            assertThat(context).doesNotHaveBean(PersonalIntentRouter.class);
                            assertThat(context).doesNotHaveBean(PerUserRateLimiter.class);
                        });
    }

    @Test
    @DisplayName("챗봇 자체가 꺼져 있으면 개인화 플래그만 켜도 아무것도 만들어지지 않는다")
    void personalNeedsChatbot() {
        runner().withPropertyValues("CHATBOT_PERSONAL_ENABLED=true")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).doesNotHaveBean(PersonalChatbotService.class);
                        });
    }

    @Test
    @DisplayName("두 플래그를 모두 켜면 개인화 빈이 만들어지고 사용자별 한도 기본값은 분당 10회다")
    void personalOn() {
        runner().withPropertyValues(
                        "CHATBOT_ENABLED=true",
                        "CHATBOT_PERSONAL_ENABLED=true",
                        "CHATBOT_EMBEDDING_PROVIDER=ngram")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(PersonalChatbotService.class);
                            assertThat(context).hasSingleBean(PersonalIntentRouter.class);
                            PerUserRateLimiter limiter = context.getBean(PerUserRateLimiter.class);
                            for (int i = 0; i < 10; i++) {
                                assertThat(limiter.tryAcquire(1L)).isTrue();
                            }
                            assertThat(limiter.tryAcquire(1L)).isFalse();
                        });
    }

    @Test
    @DisplayName("한도 설정 값을 바꿀 수 있고, 0 이하면 부팅이 실패한다")
    void rateLimitConfigurable() {
        runner().withPropertyValues(
                        "CHATBOT_ENABLED=true",
                        "CHATBOT_PERSONAL_ENABLED=true",
                        "CHATBOT_EMBEDDING_PROVIDER=ngram",
                        "CHATBOT_PERSONAL_RATE_PER_MINUTE=2")
                .run(
                        context -> {
                            PerUserRateLimiter limiter = context.getBean(PerUserRateLimiter.class);
                            assertThat(limiter.tryAcquire(1L)).isTrue();
                            assertThat(limiter.tryAcquire(1L)).isTrue();
                            assertThat(limiter.tryAcquire(1L)).isFalse();
                        });
        runner().withPropertyValues(
                        "CHATBOT_ENABLED=true",
                        "CHATBOT_PERSONAL_ENABLED=true",
                        "CHATBOT_EMBEDDING_PROVIDER=ngram",
                        "CHATBOT_PERSONAL_RATE_PER_MINUTE=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
