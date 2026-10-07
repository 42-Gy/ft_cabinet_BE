package com.gyeongsan.cabinet.adapter.in.web.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.adapter.in.web.user.dto.MyProfileResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MyProfileResponseDtoJsonTest {

    @Test
    @DisplayName("/me JSON 에 isTranscender 키가 boolean 으로 나간다 (실제 앱과 같은 Boot ObjectMapper)")
    void serializesIsTranscenderKey() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(
                        context -> {
                            ObjectMapper mapper = context.getBean(ObjectMapper.class);

                            JsonNode transcender =
                                    mapper.valueToTree(
                                            MyProfileResponseDto.builder()
                                                    .transcender(true)
                                                    .build());
                            JsonNode general =
                                    mapper.valueToTree(
                                            MyProfileResponseDto.builder()
                                                    .transcender(false)
                                                    .build());

                            assertThat(transcender.get("isTranscender").isBoolean()).isTrue();
                            assertThat(transcender.get("isTranscender").asBoolean()).isTrue();
                            assertThat(general.get("isTranscender").asBoolean()).isFalse();
                            // 키가 중복으로 나가지 않는다.
                            assertThat(transcender.has("transcender")).isFalse();
                        });
    }

    @Test
    @DisplayName("참고: 기존 isPisciner 필드의 JSON 키는 Lombok 게터 때문에 pisciner 이다 (변경하지 않음)")
    void existingPiscinerKeyIsDocumented() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(
                        context -> {
                            JsonNode json =
                                    context.getBean(ObjectMapper.class)
                                            .valueToTree(
                                                    MyProfileResponseDto.builder()
                                                            .isPisciner(true)
                                                            .build());

                            assertThat(json.has("pisciner")).isTrue();
                            assertThat(json.has("isPisciner")).isFalse();
                        });
    }

    @Test
    @DisplayName("/me JSON 에 대여 시작·만료 시각이 ISO 문자열로 나가고 기존 표시용 문자열 키도 그대로다")
    void serializesIsoTimestampsAlongsideLegacyStrings() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(
                        context -> {
                            JsonNode json =
                                    context.getBean(ObjectMapper.class)
                                            .valueToTree(
                                                    MyProfileResponseDto.builder()
                                                            .lentStartedAt("09월 07일 10:00")
                                                            .lentStartedAtIso(
                                                                    java.time.LocalDateTime.of(
                                                                            2026, 9, 7, 10, 0))
                                                            .expiredAt("10월 08일 10:00")
                                                            .expiredAtIso(
                                                                    java.time.LocalDateTime.of(
                                                                            2026, 10, 8, 10, 0))
                                                            .build());

                            assertThat(json.get("lentStartedAtIso").asText())
                                    .isEqualTo("2026-09-07T10:00:00");
                            assertThat(json.get("expiredAtIso").asText())
                                    .isEqualTo("2026-10-08T10:00:00");
                            assertThat(json.get("lentStartedAt").asText())
                                    .isEqualTo("09월 07일 10:00");
                            assertThat(json.get("expiredAt").asText()).isEqualTo("10월 08일 10:00");
                        });
    }
}
