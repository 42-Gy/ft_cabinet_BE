package com.gyeongsan.cabinet.adapter.in.web.lent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.domain.lent.port.in.LentUseCase;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterAutoConfiguration;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RestController;

class LentControllerRateLimitTest {

    private static final Path MAIN_YML = Path.of("src/main/resources/application.yml");

    @Configuration
    static class ControllerConfig {
        @Bean
        LentController lentController() {
            return new LentController(mock(LentUseCase.class), mock(UserRepositoryPort.class));
        }
    }

    private static UserPrincipal principal() {
        User user = User.of("intra01", "intra01@example.com", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        return new UserPrincipal(user, Map.of());
    }

    private static StandardEnvironment mainYmlEnvironment() throws IOException {
        StandardEnvironment env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        sources.addFirst(new MapPropertySource("empty", Map.of()));
        for (PropertySource<?> source :
                new YamlPropertySourceLoader()
                        .load("main-application-yml", new FileSystemResource(MAIN_YML))) {
            sources.addLast(source);
        }
        return env;
    }

    @Test
    @DisplayName(
            "LentController 의 모든 엔드포인트는 lentApi 한도를 함께 쓰고, 한도를 넘으면 RequestNotPermitted(429)가 난다")
    void lentApiLimitIsAppliedToAllLentEndpoints() {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                AopAutoConfiguration.class, RateLimiterAutoConfiguration.class))
                .withUserConfiguration(ControllerConfig.class)
                // 한도 값 자체가 아니라 "적용되는지"를 보기 위해, 갱신되지 않는 작은 한도로 덮어쓴다.
                .withPropertyValues(
                        "resilience4j.ratelimiter.instances.lentApi.limitForPeriod=3",
                        "resilience4j.ratelimiter.instances.lentApi.limitRefreshPeriod=1h",
                        "resilience4j.ratelimiter.instances.lentApi.timeoutDuration=0s")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            LentController controller = context.getBean(LentController.class);
                            UserPrincipal principal = principal();

                            controller.makeReservation(101, principal);
                            controller.makeReservation(102, principal);
                            controller.useExtension(principal);

                            // 같은 한도를 예약 말고 다른 엔드포인트도 함께 쓴다.
                            assertThatThrownBy(() -> controller.makeReservation(103, principal))
                                    .isInstanceOf(RequestNotPermitted.class);
                            assertThatThrownBy(() -> controller.useExtension(principal))
                                    .isInstanceOf(RequestNotPermitted.class);
                        });
    }

    @Test
    @DisplayName("한도 안에서는 요청이 막히지 않는다")
    void requestsWithinLimitAreNotBlocked() {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                AopAutoConfiguration.class, RateLimiterAutoConfiguration.class))
                .withUserConfiguration(ControllerConfig.class)
                .withPropertyValues(
                        "resilience4j.ratelimiter.instances.lentApi.limitForPeriod=50",
                        "resilience4j.ratelimiter.instances.lentApi.limitRefreshPeriod=1h",
                        "resilience4j.ratelimiter.instances.lentApi.timeoutDuration=0s")
                .run(
                        context -> {
                            LentController controller = context.getBean(LentController.class);
                            for (int i = 0; i < 50; i++) {
                                controller.makeReservation(100 + i, principal());
                            }
                        });
    }

    @Test
    @DisplayName("모든 컨트롤러의 @RateLimiter 이름은 application.yml 에 정의되어 있다(오타나 누락 방지)")
    void everyRateLimiterNameIsDefinedInYml() throws Exception {
        Set<String> defined =
                Binder.get(mainYmlEnvironment())
                        .bind("resilience4j.ratelimiter.instances", Map.class)
                        .map(m -> (Set<String>) new TreeSet<String>(m.keySet()))
                        .get();
        assertThat(defined).contains("lentApi");

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<String> used = new TreeSet<>();
        for (BeanDefinition candidate :
                scanner.findCandidateComponents("com.gyeongsan.cabinet.adapter.in.web")) {
            Class<?> type = Class.forName(candidate.getBeanClassName());
            RateLimiter annotation = type.getAnnotation(RateLimiter.class);
            if (annotation != null) {
                used.add(annotation.name());
            }
        }
        assertThat(used).contains("lentApi", "userApi");
        assertThat(defined).containsAll(used);
    }

    @Test
    @DisplayName("lentApi 한도는 양수이고, 요청이 막히지 않도록 갱신 주기가 설정되어 있다")
    void lentApiConfigIsSane() throws IOException {
        StandardEnvironment env = mainYmlEnvironment();

        Integer limit =
                env.getProperty(
                        "resilience4j.ratelimiter.instances.lentApi.limitForPeriod", Integer.class);
        assertThat(limit).isNotNull().isPositive();
        assertThat(env.getProperty("resilience4j.ratelimiter.instances.lentApi.limitRefreshPeriod"))
                .isNotBlank();
    }
}
