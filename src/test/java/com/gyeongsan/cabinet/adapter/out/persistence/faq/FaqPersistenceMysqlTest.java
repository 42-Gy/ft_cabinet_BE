package com.gyeongsan.cabinet.adapter.out.persistence.faq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.adapter.out.embedding.CharNgramEmbeddingAdapter;
import com.gyeongsan.cabinet.application.chatbot.ChatbotService;
import com.gyeongsan.cabinet.application.chatbot.ChatbotSettings;
import com.gyeongsan.cabinet.application.chatbot.FaqIndexManager;
import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.support.BaselinedSchema;
import com.gyeongsan.cabinet.support.MariaDbDriverMySqlContainer;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 MySQL(운영 8.0, 테스트 8.4)에서 Flyway V5 와 엔티티 매핑(validate), 어댑터 동작, 검색 흐름 전체를 확인한다. Docker 가 없으면
 * 건너뛴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class FaqPersistenceMysqlTest {

    @Configuration
    @EntityScan(basePackageClasses = FaqEntity.class)
    @EnableJpaRepositories(basePackageClasses = FaqJpaRepository.class)
    static class JpaConfig {
        @Bean
        FaqPersistenceAdapter faqPersistenceAdapter(FaqJpaRepository repository) {
            return new FaqPersistenceAdapter(repository);
        }
    }

    private static ApplicationContextRunner runner(MariaDbDriverMySqlContainer mysql) {
        return new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                DataSourceAutoConfiguration.class,
                                DataSourceTransactionManagerAutoConfiguration.class,
                                TransactionAutoConfiguration.class,
                                FlywayAutoConfiguration.class,
                                HibernateJpaAutoConfiguration.class))
                .withUserConfiguration(JpaConfig.class)
                .withPropertyValues(
                        "spring.datasource.url=" + mysql.getJdbcUrl(),
                        "spring.datasource.username=" + mysql.getUsername(),
                        "spring.datasource.password=" + mysql.getPassword(),
                        "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
                        "spring.flyway.enabled=true",
                        "spring.flyway.locations=classpath:db/migration",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect");
    }

    private static Faq faq(
            String seedKey, String category, String answer, boolean enabled, String... questions) {
        return new Faq(null, seedKey, category, answer, enabled, List.of(questions), null, null);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("저장/조회/수정/삭제, 질문 순서, 사용 중 필터, 변경 감지 값(fingerprint), 시드 키 유일성")
    void crud(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            BaselinedSchema.prepare(mysql);
            runner(mysql)
                    .run(
                            context -> {
                                assertThat(context).hasNotFailed();
                                FaqPersistenceAdapter adapter =
                                        context.getBean(FaqPersistenceAdapter.class);
                                String emptyFingerprint = adapter.fingerprint();

                                Faq saved =
                                        adapter.save(
                                                faq(
                                                        "seed-1",
                                                        "대여",
                                                        "대여 답변\n두 번째 줄 😀",
                                                        true,
                                                        "사물함 대여 방법",
                                                        "사물함 빌리는 법",
                                                        "대여권이 없어요"));
                                assertThat(saved.id()).isNotNull();
                                assertThat(saved.createdAt()).isNotNull();
                                assertThat(adapter.fingerprint()).isNotEqualTo(emptyFingerprint);

                                Faq loaded = adapter.findById(saved.id()).orElseThrow();
                                assertThat(loaded.answer()).isEqualTo("대여 답변\n두 번째 줄 😀");
                                assertThat(loaded.questions())
                                        .containsExactly("사물함 대여 방법", "사물함 빌리는 법", "대여권이 없어요");
                                assertThat(loaded.seedKey()).isEqualTo("seed-1");

                                // 수정: 질문 표현을 교체하고(순서 포함) 시각이 갱신된다
                                String beforeUpdate = adapter.fingerprint();
                                Thread.sleep(5);
                                Faq updated =
                                        adapter.save(
                                                new Faq(
                                                        saved.id(),
                                                        saved.seedKey(),
                                                        "반납",
                                                        "수정된 답",
                                                        false,
                                                        List.of("새 질문 B", "새 질문 A"),
                                                        saved.createdAt(),
                                                        null));
                                assertThat(updated.questions()).containsExactly("새 질문 B", "새 질문 A");
                                assertThat(updated.createdAt()).isEqualTo(saved.createdAt());
                                assertThat(updated.updatedAt()).isAfter(saved.updatedAt());
                                assertThat(adapter.fingerprint()).isNotEqualTo(beforeUpdate);

                                // 사용 중 필터
                                adapter.save(faq(null, "연장", "연장 답", true, "연장 방법"));
                                assertThat(adapter.findAll()).hasSize(2);
                                assertThat(adapter.findAllEnabled())
                                        .extracting(Faq::category)
                                        .containsExactly("연장");

                                // 시드 키는 유일하다(여러 서버가 동시에 시드해도 하나만 들어간다)
                                assertThatThrownBy(
                                                () ->
                                                        adapter.save(
                                                                faq(
                                                                        "seed-1", "중복", "x", true,
                                                                        "q")))
                                        .isInstanceOf(Exception.class);
                                // 시드 키가 없는 항목은 여러 개 가능
                                adapter.save(faq(null, "기타", "y", true, "z"));
                                assertThat(adapter.count()).isEqualTo(3);

                                // 삭제: 질문도 함께 사라진다(고아 행 없음)
                                assertThat(adapter.deleteById(saved.id())).isTrue();
                                assertThat(adapter.deleteById(saved.id())).isFalse();
                                assertThat(adapter.findById(saved.id())).isEmpty();
                                assertThat(adapter.count()).isEqualTo(2);
                            });
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName("DB 의 FAQ 로 색인을 만들어 질문하면 찾고, 관리자가 고치면 색인도 따라온다 (ngram 기준선으로 전체 흐름 확인)")
    void endToEndWithRealDatabase(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            BaselinedSchema.prepare(mysql);
            runner(mysql)
                    .run(
                            context -> {
                                FaqPersistenceAdapter adapter =
                                        context.getBean(FaqPersistenceAdapter.class);
                                adapter.save(
                                        faq(null, "대여", "대여는 이렇게 합니다", true, "사물함 대여 방법 알려주세요"));
                                adapter.save(
                                        faq(null, "반납", "반납은 이렇게 합니다", true, "사물함 반납 방법 알려주세요"));

                                CharNgramEmbeddingAdapter embedding =
                                        new CharNgramEmbeddingAdapter();
                                FaqIndexManager manager = new FaqIndexManager(adapter, embedding);
                                ChatbotService service =
                                        new ChatbotService(
                                                manager,
                                                embedding,
                                                result -> {},
                                                new ChatbotSettings(0.55, 0.30, 3, 200, 2, "못 찾음"));
                                manager.rebuild();

                                ChatbotAnswer found = service.ask("사물함 반납 방법");
                                assertThat(found.result()).isEqualTo(ChatbotAnswer.Result.MATCHED);
                                assertThat(found.faq().answer()).isEqualTo("반납은 이렇게 합니다");

                                // 관리자가 답변을 고치고 새 FAQ 를 추가하면, 변경 감지 후 색인이 갱신된다
                                Faq returnFaq = found.faq();
                                adapter.save(
                                        new Faq(
                                                returnFaq.id(),
                                                null,
                                                "반납",
                                                "고친 반납 답변",
                                                true,
                                                returnFaq.questions(),
                                                returnFaq.createdAt(),
                                                null));
                                manager.rebuild();
                                assertThat(service.ask("사물함 반납 방법").faq().answer())
                                        .isEqualTo("고친 반납 답변");
                            });
        }
    }
}
