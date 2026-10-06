package com.gyeongsan.cabinet.application.alarm;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelException;
import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelMessage;
import com.gyeongsan.cabinet.domain.alarm.model.SlackHistory;
import com.gyeongsan.cabinet.domain.alarm.port.out.AlarmPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.ReportCursorPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.SlackChannelPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlackReportForwardServiceTest {

    private static final String CHANNEL = "C0REPORT";

    /** 채널 안의 메시지를 들고 있다가 커서보다 새로운 것만 돌려주는 가짜 슬랙. */
    static class FakeChannel implements SlackChannelPort {
        final List<SlackChannelMessage> messages = new ArrayList<>();
        boolean failFetch;
        boolean failLatest;
        boolean truncated;
        String permalinkBase = "https://workspace.slack.com/archives/" + CHANNEL + "/p";
        int fetchCalls;

        @Override
        public SlackHistory fetchNewerThan(String channelId, String oldestTs) {
            fetchCalls++;
            if (failFetch) {
                throw new SlackChannelException("boom");
            }
            BigDecimal oldest = new BigDecimal(oldestTs);
            List<SlackChannelMessage> newer =
                    messages.stream()
                            .filter(m -> new BigDecimal(m.ts()).compareTo(oldest) > 0)
                            .toList();
            return new SlackHistory(newer, truncated);
        }

        @Override
        public Optional<String> latestTs(String channelId) {
            if (failLatest) {
                throw new SlackChannelException("boom");
            }
            return messages.stream().map(SlackChannelMessage::ts).reduce((a, b) -> b);
        }

        @Override
        public Optional<String> permalink(String channelId, String ts) {
            return Optional.of(permalinkBase + ts.replace(".", ""));
        }
    }

    static class FakeCursor implements ReportCursorPort {
        final Map<String, String> store = new HashMap<>();
        boolean failRead;
        boolean failWrite;

        @Override
        public Optional<String> getCursor(String channelId) {
            if (failRead) {
                throw new IllegalStateException("redis down");
            }
            return Optional.ofNullable(store.get(channelId));
        }

        @Override
        public void saveCursor(String channelId, String ts) {
            if (failWrite) {
                throw new IllegalStateException("redis down");
            }
            store.put(channelId, ts);
        }
    }

    static class RecordingAlarm implements AlarmPort {
        record Sent(String to, String text) {}

        final List<Sent> sent = new ArrayList<>();
        final List<String> failFor = new ArrayList<>();

        @Override
        public void sendDm(String intraId, String message) {
            if (failFor.contains(intraId)) {
                throw new IllegalStateException("slack down");
            }
            sent.add(new Sent(intraId, message));
        }
    }

    private FakeChannel channel;
    private FakeCursor cursor;
    private RecordingAlarm alarm;
    private SlackReportForwardService service;

    @BeforeEach
    void setUp() {
        channel = new FakeChannel();
        cursor = new FakeCursor();
        alarm = new RecordingAlarm();
        service =
                serviceWith(new SlackReportSettings(CHANNEL, List.of("admin1", "admin2"), 20, 100));
    }

    private SlackReportForwardService serviceWith(SlackReportSettings settings) {
        return new SlackReportForwardService(
                channel,
                cursor,
                alarm,
                settings,
                Clock.fixed(Instant.ofEpochSecond(1_700_000_500L), ZoneOffset.UTC));
    }

    private static SlackChannelMessage msg(String ts, String text) {
        return new SlackChannelMessage(ts, "U123", text, null, 0, 0);
    }

    private static SlackChannelMessage system(String ts, String subtype) {
        return new SlackChannelMessage(ts, "U123", "joined", subtype, 0, 0);
    }

    @Test
    @DisplayName("처음 켜면 과거 글은 전달하지 않고 채널의 가장 최근 글 시점에서 시작한다")
    void firstRunStartsFromLatestWithoutForwardingHistory() {
        channel.messages.add(msg("1700000100.000100", "과거 글 1"));
        channel.messages.add(msg("1700000200.000100", "과거 글 2"));

        service.forwardNewReports();

        assertThat(alarm.sent).isEmpty();
        assertThat(cursor.store).containsEntry(CHANNEL, "1700000200.000100");

        // 이후 새 글만 전달된다.
        channel.messages.add(msg("1700000300.000100", "새 글"));
        service.forwardNewReports();
        assertThat(alarm.sent).hasSize(2); // 관리자 2명
        assertThat(alarm.sent.get(0).text()).contains("새 글").doesNotContain("과거 글");
    }

    @Test
    @DisplayName("채널이 비어 있는 상태로 처음 켜면 현재 시각에서 시작한다")
    void firstRunOnEmptyChannelUsesNow() {
        service.forwardNewReports();

        assertThat(cursor.store).containsEntry(CHANNEL, "1700000500.000000");
        assertThat(alarm.sent).isEmpty();
    }

    @Test
    @DisplayName("새 글은 설정된 관리자 모두에게 오래된 순서로 한 번씩 전달된다")
    void forwardsToAllRecipientsInOrder() {
        cursor.store.put(CHANNEL, "1700000000.000000");
        channel.messages.add(msg("1700000100.000100", "첫째"));
        channel.messages.add(msg("1700000200.000100", "둘째"));

        service.forwardNewReports();

        assertThat(alarm.sent)
                .extracting(RecordingAlarm.Sent::to)
                .containsExactly("admin1", "admin2", "admin1", "admin2");
        assertThat(alarm.sent.get(0).text()).contains("첫째");
        assertThat(alarm.sent.get(2).text()).contains("둘째");
        assertThat(cursor.store).containsEntry(CHANNEL, "1700000200.000100");
    }

    @Test
    @DisplayName("이미 전달한 글은 다시 전달하지 않는다 (같은 주기를 여러 번 돌려도)")
    void doesNotForwardTwice() {
        cursor.store.put(CHANNEL, "1700000000.000000");
        channel.messages.add(msg("1700000100.000100", "한 번만"));

        service.forwardNewReports();
        service.forwardNewReports();
        service.forwardNewReports();

        assertThat(alarm.sent).hasSize(2);
    }

    @Test
    @DisplayName("시스템 메시지(입퇴장, 주제 변경 등)는 전달하지 않지만 커서는 그 뒤로 옮긴다")
    void skipsSystemMessagesButAdvancesCursor() {
        cursor.store.put(CHANNEL, "1700000000.000000");
        channel.messages.add(msg("1700000100.000100", "진짜 제보"));
        channel.messages.add(system("1700000200.000100", "channel_join"));
        channel.messages.add(system("1700000300.000100", "channel_topic"));

        service.forwardNewReports();

        assertThat(alarm.sent).hasSize(2);
        assertThat(alarm.sent.get(0).text()).contains("진짜 제보");
        assertThat(cursor.store).containsEntry(CHANNEL, "1700000300.000100");

        service.forwardNewReports();
        assertThat(alarm.sent).hasSize(2);
    }

    @Test
    @DisplayName("봇이 올린 글이나 파일 첨부 글처럼 시스템 메시지가 아닌 것은 모두 전달한다")
    void forwardsEverythingThatIsNotSystem() {
        cursor.store.put(CHANNEL, "1700000000.000000");
        channel.messages.add(
                new SlackChannelMessage("1700000100.000100", "B999", "봇 글", "bot_message", 0, 0));
        channel.messages.add(
                new SlackChannelMessage("1700000200.000100", "U1", "사진", "file_share", 0, 2));
        channel.messages.add(
                new SlackChannelMessage(
                        "1700000300.000100", "U1", "스레드", "thread_broadcast", 3, 0));

        service.forwardNewReports();

        assertThat(alarm.sent).hasSize(6);
        assertThat(alarm.sent.get(2).text()).contains("첨부 파일 2개");
        assertThat(alarm.sent.get(4).text()).contains("스레드 답글 3개");
    }

    @Test
    @DisplayName("DM 에는 작성자, 시각(KST), 본문, 원문 링크가 담긴다")
    void messageFormat() {
        cursor.store.put(CHANNEL, "1700000000.000000");
        channel.messages.add(msg("1700000100.000100", "3층 12번 사물함 잠금이 안 열려요"));

        service.forwardNewReports();

        String text = alarm.sent.get(0).text();
        assertThat(text)
                .contains("사물함 오류제보")
                .contains("<@U123>")
                .contains("2023-11-15 07:15") // 1700000100 = 2023-11-14 22:15:00 UTC = KST 07:15
                .contains("3층 12번 사물함 잠금이 안 열려요")
                .contains("https://workspace.slack.com/archives/C0REPORT/p1700000100000100");
    }

    @Test
    @DisplayName("전체 멘션은 평문으로 바꾸고, 너무 긴 본문은 잘라낸다")
    void sanitizesAndTruncates() {
        cursor.store.put(CHANNEL, "1700000000.000000");
        channel.messages.add(
                msg("1700000100.000100", "<!channel> 긴급 <!here|here> " + "가".repeat(300)));

        service.forwardNewReports();

        String text = alarm.sent.get(0).text();
        assertThat(text).contains("@channel 긴급 @here").doesNotContain("<!");
        assertThat(text).contains("…(이하 원문에서 확인)");
        assertThat(text).doesNotContain("가".repeat(101));
    }

    @Test
    @DisplayName("한 번에 너무 많이 쌓였으면 최근 글만 전달하고 오래된 글은 요약 한 줄로 대신한다")
    void capsBacklogWithSummary() {
        service = serviceWith(new SlackReportSettings(CHANNEL, List.of("admin1"), 3, 100));
        cursor.store.put(CHANNEL, "1700000000.000000");
        for (int i = 1; i <= 10; i++) {
            channel.messages.add(msg("17000001%02d.000100".formatted(i), "글" + i));
        }

        service.forwardNewReports();

        assertThat(alarm.sent).hasSize(4); // 요약 1 + 전달 3
        assertThat(alarm.sent.get(0).text()).contains("이전 7건").contains("생략");
        assertThat(alarm.sent.get(1).text()).contains("글8");
        assertThat(alarm.sent.get(3).text()).contains("글10");
        assertThat(cursor.store).containsEntry(CHANNEL, "1700000110.000100");

        service.forwardNewReports();
        assertThat(alarm.sent).hasSize(4);
    }

    @Test
    @DisplayName("조회 한도를 넘겨 일부를 못 가져왔으면 '이상'으로 표시한다")
    void truncatedHistoryIsFlagged() {
        cursor.store.put(CHANNEL, "1700000000.000000");
        channel.truncated = true;
        channel.messages.add(msg("1700000100.000100", "글"));

        service.forwardNewReports();

        assertThat(alarm.sent.get(0).text()).contains("건 이상");
    }

    @Test
    @DisplayName("채널 조회에 실패하면 아무것도 전달하지 않고 커서도 그대로 둔다 (다음 주기에 재시도)")
    void fetchFailureKeepsCursor() {
        cursor.store.put(CHANNEL, "1700000000.000000");
        channel.messages.add(msg("1700000100.000100", "글"));
        channel.failFetch = true;

        service.forwardNewReports();

        assertThat(alarm.sent).isEmpty();
        assertThat(cursor.store).containsEntry(CHANNEL, "1700000000.000000");

        channel.failFetch = false;
        service.forwardNewReports();
        assertThat(alarm.sent).hasSize(2);
    }

    @Test
    @DisplayName("커서를 읽지 못하면(Redis 장애) 처음으로 착각해 시작점을 옮기지 않고 건너뛴다")
    void cursorReadFailureDoesNotReseed() {
        cursor.failRead = true;
        channel.messages.add(msg("1700000100.000100", "글"));

        service.forwardNewReports();

        assertThat(alarm.sent).isEmpty();
        assertThat(cursor.store).isEmpty();
        assertThat(channel.fetchCalls).isZero();
    }

    @Test
    @DisplayName("한 관리자에게 DM 이 실패해도 나머지에게는 전달하고 처리는 계속된다")
    void oneRecipientFailureDoesNotBlockOthers() {
        cursor.store.put(CHANNEL, "1700000000.000000");
        channel.messages.add(msg("1700000100.000100", "글"));
        alarm.failFor.add("admin1");

        service.forwardNewReports();

        assertThat(alarm.sent).extracting(RecordingAlarm.Sent::to).containsExactly("admin2");
        assertThat(cursor.store).containsEntry(CHANNEL, "1700000100.000100");
    }

    @Test
    @DisplayName("커서 저장에 실패해도 예외로 끝나지 않는다")
    void cursorWriteFailureIsTolerated() {
        cursor.store.put(CHANNEL, "1700000000.000000");
        channel.messages.add(msg("1700000100.000100", "글"));
        cursor.failWrite = true;

        service.forwardNewReports();

        assertThat(alarm.sent).hasSize(2);
    }

    @Test
    @DisplayName("처음 켤 때 시작 지점을 정하지 못하면(슬랙 오류) 커서를 만들지 않고 다음 주기에 다시 시도한다")
    void seedFailureRetriesLater() {
        channel.failLatest = true;

        service.forwardNewReports();

        assertThat(cursor.store).isEmpty();
        channel.failLatest = false;
        channel.messages.add(msg("1700000100.000100", "글"));
        service.forwardNewReports();
        assertThat(cursor.store).containsEntry(CHANNEL, "1700000100.000100");
        assertThat(alarm.sent).isEmpty();
    }

    @Test
    @DisplayName("설정 검증: 채널 ID 나 수신자가 없거나 한도가 0 이하이면 거부한다")
    void settingsValidation() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new SlackReportSettings(" ", List.of("a"), 1, 1));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new SlackReportSettings("C1", List.of(), 1, 1));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new SlackReportSettings("C1", List.of("a"), 0, 1));
    }
}
