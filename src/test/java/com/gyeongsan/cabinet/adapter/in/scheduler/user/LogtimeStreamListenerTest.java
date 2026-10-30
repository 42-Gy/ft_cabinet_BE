package com.gyeongsan.cabinet.adapter.in.scheduler.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.gyeongsan.cabinet.domain.item.port.out.ItemRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.FtApiPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.port.in.UserUseCase;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class LogtimeStreamListenerTest {

    private FtApiPort ftApiPort;
    private UserUseCase userUseCase;
    private LogtimeStreamListener listener;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ftApiPort = mock(FtApiPort.class);
        userUseCase = mock(UserUseCase.class);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        given(redisTemplate.opsForStream()).willReturn(mock(StreamOperations.class));
        listener =
                new LogtimeStreamListener(
                        ftApiPort, userUseCase, mock(ItemRepositoryPort.class), redisTemplate);
    }

    private MapRecord<String, String, String> message(boolean isPayDay) {
        return StreamRecords.newRecord()
                .in("logtime-sync-stream")
                .ofMap(
                        Map.of(
                                "userId", "1",
                                "intraId", "tester",
                                "start", "2026-10-01T00:00",
                                "end", "2026-10-31T23:59:59",
                                "isPayDay", String.valueOf(isPayDay)));
    }

    @Test
    @DisplayName("1일에 API가 실패해도 월간 보상 처리를 호출한다")
    void payDayApiFailure_stillProcessesReward() {
        given(ftApiPort.getLogtimeBetween(any(), any(), any())).willReturn(-1);

        listener.onMessage(message(true));

        verify(userUseCase).processLogtimeTransaction(1L, null, -1, true);
    }

    @Test
    @DisplayName("평일에 API가 실패하면 처리하지 않고 건너뛴다")
    void normalDayApiFailure_skips() {
        given(ftApiPort.getLogtimeBetween(any(), any(), any())).willReturn(-1);

        listener.onMessage(message(false));

        verify(userUseCase, never())
                .processLogtimeTransaction(any(), any(), anyInt(), anyBoolean());
    }

    @Test
    @DisplayName("동시 수정 충돌이 나면 한 번 더 시도한다")
    void optimisticLockConflict_retriesOnce() {
        given(ftApiPort.getLogtimeBetween(any(), any(), any())).willReturn(300);
        doThrow(new ObjectOptimisticLockingFailureException(User.class, 1L))
                .doNothing()
                .when(userUseCase)
                .processLogtimeTransaction(1L, null, 300, false);

        listener.onMessage(message(false));

        verify(userUseCase, times(2)).processLogtimeTransaction(1L, null, 300, false);
    }
}
