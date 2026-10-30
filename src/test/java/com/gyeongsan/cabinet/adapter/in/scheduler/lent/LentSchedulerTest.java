package com.gyeongsan.cabinet.adapter.in.scheduler.lent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.gyeongsan.cabinet.alarm.dto.AlarmEvent;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class LentSchedulerTest {

    @Mock private LentRepositoryPort lentRepository;
    @Mock private ItemHistoryRepositoryPort itemHistoryRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserRepositoryPort userRepository;

    @InjectMocks private LentScheduler lentScheduler;

    @Test
    @DisplayName("checkOverdue: 연체 알림에는 사물함 표시 번호가 들어간다")
    void checkOverdue_alarmUsesVisibleNum() {
        User user = User.builder().id(1L).name("tester").role(UserRole.USER).build();
        Cabinet cabinet =
                Cabinet.of(42, CabinetStatus.FULL, LentType.PRIVATE, 1, null, 1, "A", 1, 1);
        LocalDateTime expiredAt = LocalDateTime.now().minusDays(1);
        LentHistory lent = LentHistory.of(user, cabinet, expiredAt.minusDays(30), expiredAt);
        given(lentRepository.findAllOverdueLentHistories(any())).willReturn(List.of(lent));

        lentScheduler.checkOverdue();

        ArgumentCaptor<AlarmEvent> event = ArgumentCaptor.forClass(AlarmEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().getMessage()).contains("42번 사물함");
        assertThat(cabinet.getStatus()).isEqualTo(CabinetStatus.OVERDUE);
    }
}
