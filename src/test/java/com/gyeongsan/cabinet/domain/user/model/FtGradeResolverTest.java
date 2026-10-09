package com.gyeongsan.cabinet.domain.user.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.support.FtFixtures;
import com.gyeongsan.cabinet.utils.FtCursusParser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FtGradeResolverTest {

    private static FtGradeSnapshot resolve(FtCursusEntry... entries) {
        return FtGradeResolver.resolve(Optional.of(List.of(entries)));
    }

    private static User userWithGrade(String grade) {
        User user = User.of("test-user", "test@example.com", UserRole.USER);
        user.updateFtGrade(grade);
        return user;
    }

    @Test
    @DisplayName("실제 응답(트센): cursus 21 의 Transcender 만 읽고 9, 66 은 무시한다")
    void transcenderFixture() {
        FtGradeSnapshot snapshot =
                FtGradeResolver.resolve(
                        FtCursusParser.fromAttribute(
                                FtFixtures.asAttributes(FtFixtures.TRANSCENDER)
                                        .get("cursus_users")));

        assertThat(snapshot.parsed()).isTrue();
        assertThat(snapshot.grade()).isEqualTo("Transcender");
        assertThat(userWithGrade(snapshot.grade()).isTranscender()).isTrue();
    }

    @Test
    @DisplayName("실제 응답(Cadet): 일반 사용자다")
    void cadetFixture() {
        FtGradeSnapshot snapshot =
                FtGradeResolver.resolve(
                        FtCursusParser.fromAttribute(
                                FtFixtures.asAttributes(FtFixtures.CADET).get("cursus_users")));

        assertThat(snapshot.grade()).isEqualTo("Cadet");
        assertThat(userWithGrade(snapshot.grade()).isTranscender()).isFalse();
    }

    @Test
    @DisplayName("다른 cursus 의 grade 는 읽지 않는다: 피시너(9)에 Transcender 가 있어도 본과정이 아니면 무시")
    void ignoresOtherCursus() {
        FtGradeSnapshot snapshot =
                resolve(new FtCursusEntry(9, "Transcender"), new FtCursusEntry(66, "Transcender"));

        assertThat(snapshot.parsed()).isTrue();
        assertThat(snapshot.grade()).isNull();
    }

    @Test
    @DisplayName("피시너만 있는 사용자(cursus 21 없음)는 grade 가 null 이다")
    void pisciner() {
        FtGradeSnapshot snapshot = resolve(new FtCursusEntry(9, "Pisciner"));

        assertThat(snapshot.parsed()).isTrue();
        assertThat(snapshot.grade()).isNull();
    }

    @Test
    @DisplayName("본과정 항목이 여러 개여도 하나라도 Transcender 면 트센이다")
    void anyTranscenderWins() {
        assertThat(
                        resolve(
                                        new FtCursusEntry(21, "Cadet"),
                                        new FtCursusEntry(21, "Transcender"))
                                .grade())
                .isEqualTo("Transcender");
        assertThat(
                        resolve(
                                        new FtCursusEntry(21, "Transcender"),
                                        new FtCursusEntry(21, "Cadet"))
                                .grade())
                .isEqualTo("Transcender");
    }

    @Test
    @DisplayName("해석에 실패한 응답은 parsed=false 라 저장된 값을 건드리지 않는다")
    void unparsed() {
        FtGradeSnapshot snapshot = FtGradeResolver.resolve(Optional.empty());

        assertThat(snapshot.parsed()).isFalse();
        assertThat(snapshot.shouldStore()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"transcender", "TRANSCENDER", " Transcender", "Transcender ", "Member"})
    @DisplayName("합성 데이터: 정확히 Transcender 가 아닌 값은 모두 일반 사용자이고 미확인 값으로 표시된다")
    void nearMissesAreNotTranscender(String grade) {
        FtGradeSnapshot snapshot = resolve(new FtCursusEntry(21, grade));

        assertThat(userWithGrade(snapshot.grade()).isTranscender()).isFalse();
        assertThat(FtGradeResolver.isRecognized(snapshot.grade())).isFalse();
    }

    @Test
    @DisplayName("확인된 값(Cadet, Transcender)과 값 없음은 인식된 값이다")
    void recognizedValues() {
        assertThat(FtGradeResolver.isRecognized("Cadet")).isTrue();
        assertThat(FtGradeResolver.isRecognized("Transcender")).isTrue();
        assertThat(FtGradeResolver.isRecognized(null)).isTrue();
    }

    @Test
    @DisplayName("값이 비어 있으면 null 로 저장한다")
    void emptyGradeBecomesNull() {
        assertThat(resolve(new FtCursusEntry(21, "")).grade()).isNull();
        assertThat(resolve(new FtCursusEntry(21, null)).grade()).isNull();
    }

    @Test
    @DisplayName("합성 데이터: 너무 길거나 제어문자가 있거나 공백뿐인 값은 저장하지 않고 거부한다")
    void rejectsMalformedValues() {
        for (String bad : List.of("x".repeat(33), "Cad\net", "   ", "Cadet\u0000")) {
            FtGradeSnapshot snapshot = resolve(new FtCursusEntry(21, bad));

            assertThat(snapshot.rejected()).as(bad).isTrue();
            assertThat(snapshot.shouldStore()).as(bad).isFalse();
        }
        // 길이 32 는 허용한다.
        assertThat(resolve(new FtCursusEntry(21, "x".repeat(32))).shouldStore()).isTrue();
    }

    @Test
    @DisplayName("grade 가 없는 사용자와 Cadet 은 트센이 아니다")
    void userTranscenderFlag() {
        assertThat(userWithGrade(null).isTranscender()).isFalse();
        assertThat(userWithGrade("Cadet").isTranscender()).isFalse();
        assertThat(userWithGrade("Transcender").isTranscender()).isTrue();
    }
}
