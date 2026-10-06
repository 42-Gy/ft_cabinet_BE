package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.gyeongsan.cabinet.application.chatbot.ChatbotService;
import com.gyeongsan.cabinet.application.chatbot.ChatbotSettings;
import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import com.gyeongsan.cabinet.domain.chatbot.port.out.FaqRepositoryPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

class ChatbotConfigTest {

    private static final Path MAIN_YML = Path.of("src/main/resources/application.yml");

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(ChatbotConfig.class)
                .withBean(FaqRepositoryPort.class, () -> mock(FaqRepositoryPort.class))
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
    @DisplayName("운영 application.yml 기본값에서는 꺼져 있어 아무 빈도 만들어지지 않는다")
    void disabledByDefault() {
        runner().run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).doesNotHaveBean(ChatbotService.class);
                            assertThat(context).doesNotHaveBean(EmbeddingPort.class);
                        });
    }

    @Test
    @DisplayName("켜고 ngram 방식을 고르면 모델 파일 없이 빈이 만들어지고 기본 임계값이 적용된다")
    void enabledWithNgram() {
        runner().withPropertyValues("CHATBOT_ENABLED=true", "CHATBOT_EMBEDDING_PROVIDER=ngram")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(ChatbotService.class);
                            ChatbotSettings settings = context.getBean(ChatbotSettings.class);
                            assertThat(settings.matchThreshold()).isEqualTo(0.92);
                            assertThat(settings.suggestThreshold()).isEqualTo(0.88);
                            assertThat(settings.matchMargin()).isEqualTo(0.03);
                            assertThat(settings.matchAlternatives()).isEqualTo(2);
                            assertThat(settings.maxQuestionLength()).isEqualTo(200);
                            assertThat(context.getBean(EmbeddingPort.class).modelId())
                                    .startsWith("char-ngram");
                        });
    }

    @Test
    @DisplayName("켜고 onnx 방식인데 모델 파일이 없어도 서버는 뜨고, 임베딩할 때만 '사용 불가'가 된다")
    void onnxWithMissingModelStillBoots() {
        runner().withPropertyValues(
                        "CHATBOT_ENABLED=true",
                        "CHATBOT_MODEL_DIR=/definitely/not/here",
                        "CHATBOT_MODEL_ID=test-model")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            EmbeddingPort port = context.getBean(EmbeddingPort.class);
                            assertThat(port.modelId()).isEqualTo("test-model");
                            assertThatThrownBy(() -> port.embed("질문"))
                                    .isInstanceOf(EmbeddingUnavailableException.class);
                        });
    }

    @Test
    @DisplayName("모델 id 를 따로 설정하지 않으면 모델 디렉터리의 model.properties 를 따르고, 명시한 값이 우선한다")
    void modelIdFollowsModelPropertiesUnlessOverridden(@TempDir Path dir) throws IOException {
        Files.writeString(
                dir.resolve("model.properties"),
                "modelId=from-file\nprefix=query: \nmaxTokens=64\n",
                StandardCharsets.UTF_8);

        runner().withPropertyValues("CHATBOT_ENABLED=true", "CHATBOT_MODEL_DIR=" + dir)
                .run(
                        context ->
                                assertThat(context.getBean(EmbeddingPort.class).modelId())
                                        .isEqualTo("from-file"));
        runner().withPropertyValues(
                        "CHATBOT_ENABLED=true",
                        "CHATBOT_MODEL_DIR=" + dir,
                        "CHATBOT_MODEL_ID=explicit")
                .run(
                        context ->
                                assertThat(context.getBean(EmbeddingPort.class).modelId())
                                        .isEqualTo("explicit"));
    }

    @Test
    @DisplayName("model.properties 가 깨져 있어도 서버는 뜬다")
    void brokenModelPropertiesDoesNotBreakBoot(@TempDir Path dir) throws IOException {
        Files.writeString(
                dir.resolve("model.properties"), "maxTokens=abc\n\\u12", StandardCharsets.UTF_8);

        runner().withPropertyValues("CHATBOT_ENABLED=true", "CHATBOT_MODEL_DIR=" + dir)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("설정 실수(알 수 없는 방식, 임계값 역전)는 부팅을 실패시킨다")
    void invalidConfigurationFailsStartup() {
        runner().withPropertyValues("CHATBOT_ENABLED=true", "CHATBOT_EMBEDDING_PROVIDER=openai")
                .run(context -> assertThat(context).hasFailed());
        runner().withPropertyValues(
                        "CHATBOT_ENABLED=true",
                        "CHATBOT_EMBEDDING_PROVIDER=ngram",
                        "CHATBOT_MATCH_THRESHOLD=0.5",
                        "CHATBOT_SUGGEST_THRESHOLD=0.7")
                .run(context -> assertThat(context).hasFailed());
        runner().withPropertyValues(
                        "CHATBOT_ENABLED=true",
                        "CHATBOT_EMBEDDING_PROVIDER=ngram",
                        "CHATBOT_MATCH_MARGIN=1.5")
                .run(context -> assertThat(context).hasFailed());
        runner().withPropertyValues(
                        "CHATBOT_ENABLED=true",
                        "CHATBOT_EMBEDDING_PROVIDER=ngram",
                        "CHATBOT_MATCH_ALTERNATIVES=-1")
                .run(context -> assertThat(context).hasFailed());
    }
}
