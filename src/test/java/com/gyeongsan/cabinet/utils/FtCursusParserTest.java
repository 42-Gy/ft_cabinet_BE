package com.gyeongsan.cabinet.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.domain.user.model.FtCursusEntry;
import com.gyeongsan.cabinet.support.FtFixtures;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FtCursusParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {FtFixtures.TRANSCENDER, FtFixtures.CADET})
    @DisplayName("로그인(Map)과 재조회(JsonNode) 두 경로가 같은 결과를 낸다")
    void bothPathsAgree(String fixture) {
        Optional<List<FtCursusEntry>> fromMap =
                FtCursusParser.fromAttribute(FtFixtures.asAttributes(fixture).get("cursus_users"));
        Optional<List<FtCursusEntry>> fromJson =
                FtCursusParser.fromJson(FtFixtures.asJson(fixture).get("cursus_users"));

        assertThat(fromMap).isPresent();
        assertThat(fromMap).isEqualTo(fromJson);
    }

    @Test
    @DisplayName("실제 응답 구조: 9, 21, 66 세 항목과 grade 값을 그대로 읽는다")
    void readsFixtureEntries() {
        Optional<List<FtCursusEntry>> entries =
                FtCursusParser.fromJson(
                        FtFixtures.asJson(FtFixtures.TRANSCENDER).get("cursus_users"));

        assertThat(entries.orElseThrow())
                .containsExactly(
                        new FtCursusEntry(9, "Pisciner"),
                        new FtCursusEntry(21, "Transcender"),
                        new FtCursusEntry(66, null));
    }

    @Test
    @DisplayName("cursus_users 가 없거나 배열이 아니면 '해석 실패'(빈 Optional)이고, 빈 배열은 '항목 없음'이다")
    void distinguishesFailureFromEmpty() throws Exception {
        assertThat(FtCursusParser.fromAttribute(null)).isEmpty();
        assertThat(FtCursusParser.fromAttribute("oops")).isEmpty();
        assertThat(FtCursusParser.fromAttribute(Map.of())).isEmpty();
        assertThat(FtCursusParser.fromJson(null)).isEmpty();
        assertThat(FtCursusParser.fromJson(MAPPER.readTree("{}"))).isEmpty();
        assertThat(FtCursusParser.fromJson(MAPPER.readTree("\"x\""))).isEmpty();

        assertThat(FtCursusParser.fromAttribute(List.of())).contains(List.of());
        assertThat(FtCursusParser.fromJson(MAPPER.readTree("[]"))).contains(List.of());
    }

    @Test
    @DisplayName("합성 데이터: 형식이 깨진 개별 항목은 건너뛰고 나머지는 읽는다")
    void skipsMalformedEntries() throws Exception {
        String json =
                """
                [
                  "not-an-object",
                  {"grade": "Cadet"},
                  {"grade": "Cadet", "cursus": {"id": "21"}},
                  {"grade": 7, "cursus": {"id": 21}},
                  {"grade": "Transcender", "cursus": {"id": 21}}
                ]
                """;

        assertThat(FtCursusParser.fromJson(MAPPER.readTree(json)).orElseThrow())
                .containsExactly(new FtCursusEntry(21, null), new FtCursusEntry(21, "Transcender"));

        List<Object> attributes =
                List.of(
                        "not-an-object",
                        Map.of("grade", "Cadet"),
                        Map.of("grade", "Cadet", "cursus", Map.of("id", "21")),
                        Map.of("grade", 7, "cursus", Map.of("id", 21)),
                        Map.of("grade", "Transcender", "cursus", Map.of("id", 21)));
        assertThat(FtCursusParser.fromAttribute(attributes).orElseThrow())
                .containsExactly(new FtCursusEntry(21, null), new FtCursusEntry(21, "Transcender"));
    }
}
