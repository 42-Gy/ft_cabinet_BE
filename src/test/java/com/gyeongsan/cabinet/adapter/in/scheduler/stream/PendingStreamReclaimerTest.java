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

    private MapRecord<String, String, String> record(RecordId id) {
        return StreamRecords.newRecord().in(KEY).withId(id).ofMap(Map.of("userId", "1"));
    }

    @Test
    @DisplayName("60분 넘게 방치된 메시지만 가져와서 다시 처리한다")
    void reclaimsOnlyIdleMessages() {
        RecordId idleId =
                RecordId.of(System.currentTimeMillis() - Duration.ofMinutes(70).toMillis(), 0);
        RecordId busyId =
                RecordId.of(System.currentTimeMillis() - Duration.ofMinutes(5).toMillis(), 0);
        given(streamOps.pending(eq(KEY), eq(GROUP), any(), anyLong()))
                .willReturn(
                        new PendingMessages(
                                GROUP,
                                List.of(
                                        pending(idleId.getValue(), Duration.ofMinutes(70)),
                                        pending(busyId.getValue(), Duration.ofMinutes(5)))));
        MapRecord<String, String, String> idle = record(idleId);
        given(streamOps.claim(KEY, GROUP, "me", PendingStreamReclaimer.MIN_IDLE, idleId))
                .willReturn(List.of(idle));

        reclaimer.reclaim(KEY, logtimeListener);

        verify(logtimeListener).onMessage(idle);
        verify(streamOps, never()).delete(anyString(), any(RecordId[].class));
    }

    @Test
    @DisplayName("6시간 넘게 지난 메시지는 다시 처리하지 않고 버린다")
    void discardsStaleMessages() {
        RecordId staleId =
                RecordId.of(System.currentTimeMillis() - Duration.ofDays(5).toMillis(), 0);
        given(streamOps.pending(eq(KEY), eq(GROUP), any(), anyLong()))
                .willReturn(
                        new PendingMessages(
                                GROUP, List.of(pending(staleId.getValue(), Duration.ofDays(5)))));
        given(streamOps.claim(KEY, GROUP, "me", PendingStreamReclaimer.MIN_IDLE, staleId))
                .willReturn(List.of(record(staleId)));

        reclaimer.reclaim(KEY, logtimeListener);

        verify(logtimeListener, never()).onMessage(any());
        verify(streamOps).acknowledge(KEY, GROUP, staleId);
        verify(streamOps).delete(KEY, staleId);
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
