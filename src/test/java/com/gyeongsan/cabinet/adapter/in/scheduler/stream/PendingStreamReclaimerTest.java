package com.gyeongsan.cabinet.adapter.in.scheduler.stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gyeongsan.cabinet.adapter.in.scheduler.user.LogtimeStreamListener;
import com.gyeongsan.cabinet.alarm.SlackAlarmStreamListener;
import com.gyeongsan.cabinet.config.RedisStreamConfig;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class PendingStreamReclaimerTest {

    private static final String KEY = RedisStreamConfig.LOGTIME_STREAM_KEY;
    private static final String GROUP = RedisStreamConfig.CONSUMER_GROUP_NAME;

    private StreamOperations<String, String, String> streamOps;
    private LogtimeStreamListener logtimeListener;
    private PendingStreamReclaimer reclaimer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        streamOps = mock(StreamOperations.class);
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOps);
        RedisStreamConfig config = mock(RedisStreamConfig.class);
        given(config.getConsumerName()).willReturn("me");
        logtimeListener = mock(LogtimeStreamListener.class);
        reclaimer =
                new PendingStreamReclaimer(
                        redisTemplate,
                        config,
                        mock(SlackAlarmStreamListener.class),
                        logtimeListener);
    }

    private PendingMessage pending(String id, Duration idle) {
        return new PendingMessage(RecordId.of(id), Consumer.from(GROUP, "dead"), idle, 1);
    }

    @Test
    @DisplayName("60분 넘게 방치된 메시지만 가져와서 다시 처리한다")
    void reclaimsOnlyIdleMessages() {
        given(streamOps.pending(eq(KEY), eq(GROUP), any(), anyLong()))
                .willReturn(
                        new PendingMessages(
                                GROUP,
                                List.of(
                                        pending("1-0", Duration.ofMinutes(61)),
                                        pending("2-0", Duration.ofMinutes(5)))));
        MapRecord<String, String, String> record =
                StreamRecords.newRecord()
                        .in(KEY)
                        .withId(RecordId.of("1-0"))
                        .ofMap(Map.of("userId", "1"));
        given(
                        streamOps.claim(
                                KEY,
                                GROUP,
                                "me",
                                PendingStreamReclaimer.MIN_IDLE,
                                RecordId.of("1-0")))
                .willReturn(List.of(record));

        reclaimer.reclaim(KEY, logtimeListener);

        verify(logtimeListener).onMessage(record);
    }

    @Test
    @DisplayName("처리 중인 메시지만 있으면 아무것도 가져오지 않는다")
    void skipsWhenNothingIdle() {
        given(streamOps.pending(eq(KEY), eq(GROUP), any(), anyLong()))
                .willReturn(
                        new PendingMessages(GROUP, List.of(pending("2-0", Duration.ofMinutes(5)))));

        reclaimer.reclaim(KEY, logtimeListener);

        verify(streamOps, never())
                .claim(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(Duration.class),
                        any(RecordId[].class));
        verify(logtimeListener, never()).onMessage(any());
    }
}
