package com.gyeongsan.cabinet.application.kakaonotify;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlackMrkdwnTest {

    @Test
    @DisplayName("멘션·채널·전체 알림 표기는 읽을 수 있는 글자로 바뀐다")
    void mentions() {
        assertThat(SlackMrkdwn.toPlainText("<!channel> 공지 <!here|here> <!everyone>"))
                .isEqualTo("@channel 공지 @here @everyone");
        assertThat(SlackMrkdwn.toPlainText("<@U123> 확인 <@U456|juyoukim>"))
                .isEqualTo("@멤버 확인 @juyoukim");
        assertThat(SlackMrkdwn.toPlainText("<#C1|general> <#C2>")).isEqualTo("#general #채널");
        assertThat(SlackMrkdwn.toPlainText("<!subteam^S1|@staff>")).isEqualTo("@staff");
    }

    @Test
    @DisplayName("링크는 주소를 잃지 않게 풀고, HTML 이스케이프는 되돌린다")
    void linksAndEntities() {
        assertThat(SlackMrkdwn.toPlainText("<https://a.kr|신청서> 와 <https://b.kr>"))
                .isEqualTo("신청서(https://a.kr) 와 https://b.kr");
        assertThat(SlackMrkdwn.toPlainText("<https://a.kr|https://a.kr>"))
                .isEqualTo("https://a.kr");
        assertThat(SlackMrkdwn.toPlainText("A &amp; B &lt;ok&gt;")).isEqualTo("A & B <ok>");
        assertThat(SlackMrkdwn.toPlainText("<mailto:a@b.kr|a@b.kr>")).isEqualTo("a@b.kr");
    }

    @Test
    @DisplayName("빈 값과 과한 빈 줄을 정리한다")
    void blanks() {
        assertThat(SlackMrkdwn.toPlainText(null)).isEmpty();
        assertThat(SlackMrkdwn.toPlainText("  가\n\n\n\n나  ")).isEqualTo("가\n\n나");
    }

    @Test
    @DisplayName("자를 때는 글자(코드 포인트) 기준으로 한도 이하로 맞추고 말줄임표를 붙인다")
    void truncates() {
        assertThat(SlackMrkdwn.truncate("짧음", 10)).isEqualTo("짧음");
        String cut = SlackMrkdwn.truncate("가".repeat(300), 200);
        assertThat(cut.codePointCount(0, cut.length())).isEqualTo(200);
        assertThat(cut).endsWith("…");
        // 이모지(서로게이트 쌍)가 반으로 잘리지 않는다.
        String emoji = SlackMrkdwn.truncate("😀".repeat(50), 10);
        assertThat(emoji.codePointCount(0, emoji.length())).isEqualTo(10);
        assertThat(emoji).doesNotContain("\uD83D…");
    }
}
