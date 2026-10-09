package com.gyeongsan.cabinet.adapter.in.scheduler.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.domain.item.port.out.ItemRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.FtApiPort;
import com.gyeongsan.cabinet.domain.user.model.FtCursusEntry;
import com.gyeongsan.cabinet.domain.user.model.FtGradeSnapshot;
import com.gyeongsan.cabinet.domain.user.port.in.UserUseCase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

class LogtimeStreamListenerTest {

    private static final Long USER_ID = 9L;
    private static final String INTRA_ID = "test-user";

    private FtApiPort ftApiPort;
    private UserUseCase userUseCase;
    private LogtimeStreamListener listener;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ftApiPort = mock(FtApiPort.class);
        userUseCase = mock(UserUseCase.class);
        listener =
                new LogtimeStreamListener(
                        ftApiPort,
                        userUseCase,
                        mock(ItemRepositoryPort.class),
                        mock(RedisTemplate.class, RETURNS_DEEP_STUBS));
        when(ftApiPort.getLogtimeBetween(eq(INTRA_ID), any(), any())).thenReturn(1000);
    }

    private static MapRecord<String, String, String> message(boolean payDay) {
        Map<String, String> value =
                Map.of(
                        "userId", String.valueOf(USER_ID),
                        "intraId", INTRA_ID,
                        "start", LocalDateTime.of(2026, 9, 1, 0, 0).toString(),
                        "end", LocalDateTime.of(2026, 9, 30, 23, 59, 59).toString(),
                        "isPayDay", String.valueOf(payDay));
        return StreamRecords.mapBacked(value).withStreamKey("logtime-sync-stream");
    }

    @Test
    @DisplayName("지급일에 재조회 대상이면 grade 를 다시 조회·저장한 뒤 그 결과로 지급 처리한다")
    void refreshesGradeBeforeRewardOnPayDay() {
        when(userUseCase.needsGradeRefresh(USER_ID, 1000)).thenReturn(true);
        when(ftApiPort.getCursusEntries(INTRA_ID))
                .thenReturn(Optional.of(List.of(new FtCursusEntry(21, "Transcender"))));

        listener.onMessage(message(true));

        InOrder order = inOrder(ftApiPort, userUseCase);
        order.verify(ftApiPort).getCursusEntries(INTRA_ID);
        ArgumentCaptor<FtGradeSnapshot> snapshot = ArgumentCaptor.forClass(FtGradeSnapshot.class);
        order.verify(userUseCase).updateFtGrade(eq(USER_ID), snapshot.capture());
        order.verify(userUseCase).processLogtimeTransaction(eq(USER_ID), any(), eq(1000), eq(true));
        org.assertj.core.api.Assertions.assertThat(snapshot.getValue().grade())
                .isEqualTo("Transcender");
    }

    @Test
    @DisplayName("재조회 대상이 아니면 42 API 를 부르지 않는다")
    void noRefreshWhenNotNeeded() {
        when(userUseCase.needsGradeRefresh(USER_ID, 1000)).thenReturn(false);

        listener.onMessage(message(true));

        verify(ftApiPort, never()).getCursusEntries(any());
        verify(userUseCase, never()).updateFtGrade(anyLong(), any());
        verify(userUseCase).processLogtimeTransaction(eq(USER_ID), any(), eq(1000), eq(true));
    }

    @Test
    @DisplayName("지급일이 아니면 재조회 여부조차 묻지 않는다")
    void noRefreshOnRegularDays() {
        listener.onMessage(message(false));

        verify(userUseCase, never()).needsGradeRefresh(anyLong(), anyInt());
        verify(ftApiPort, never()).getCursusEntries(any());
        verify(userUseCase).processLogtimeTransaction(eq(USER_ID), any(), eq(1000), eq(false));
    }

    @Test
    @DisplayName("재조회 응답을 해석하지 못하면 저장하지 않고 지급 처리는 계속한다")
    void refreshFailureKeepsStoredGradeAndStillProcesses() {
        when(userUseCase.needsGradeRefresh(USER_ID, 1000)).thenReturn(true);
        when(ftApiPort.getCursusEntries(INTRA_ID)).thenReturn(Optional.empty());

        listener.onMessage(message(true));

        verify(userUseCase, never()).updateFtGrade(anyLong(), any());
        verify(userUseCase).processLogtimeTransaction(eq(USER_ID), any(), eq(1000), eq(true));
    }

    @Test
    @DisplayName("재조회 중 예외(레이트리밋 등)가 나도 지급 처리는 막지 않는다")
    void refreshExceptionDoesNotBlockReward() {
        when(userUseCase.needsGradeRefresh(USER_ID, 1000)).thenReturn(true);
        when(ftApiPort.getCursusEntries(INTRA_ID)).thenThrow(new IllegalStateException("limit"));

        listener.onMessage(message(true));

        verify(userUseCase, never()).updateFtGrade(anyLong(), any());
        verify(userUseCase).processLogtimeTransaction(eq(USER_ID), any(), eq(1000), anyBoolean());
    }

    @Test
    @DisplayName("로그타임 조회가 실패(-1)하면 재조회도 지급 처리도 하지 않는다 (기존 동작)")
    void logtimeFailureSkipsEverything() {
        when(ftApiPort.getLogtimeBetween(eq(INTRA_ID), any(), any())).thenReturn(-1);

        listener.onMessage(message(true));

        verify(userUseCase, never()).needsGradeRefresh(anyLong(), anyInt());
        verify(userUseCase, never())
                .processLogtimeTransaction(anyLong(), any(), anyInt(), anyBoolean());
    }
}
