package com.gyeongsan.cabinet.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.CabinetStatusRequest;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminCabinetServiceTest {

    @Mock private CabinetRepositoryPort cabinetRepository;
    @Mock private LentRepositoryPort lentRepository;

    @InjectMocks private AdminCabinetService adminCabinetService;

    @Test
    @DisplayName("updateCabinetStatus: updates status successfully")
    void testUpdateCabinetStatus() {
        Cabinet cabinet =
                Cabinet.of(1, CabinetStatus.AVAILABLE, LentType.PRIVATE, 1, "note", 1, "A", 1, 1);
        given(cabinetRepository.findByVisibleNumWithLock(1)).willReturn(Optional.of(cabinet));

        CabinetStatusRequest request =
                new CabinetStatusRequest(CabinetStatus.BROKEN, LentType.PRIVATE, "Test broken");
        adminCabinetService.updateCabinetStatus(1, request);

        assertThat(cabinet.getStatus()).isEqualTo(CabinetStatus.BROKEN);
        assertThat(cabinet.getLentType()).isEqualTo(LentType.PRIVATE);
        assertThat(cabinet.getStatusNote()).isEqualTo("Test broken");
    }

    @Test
    @DisplayName("updateCabinetStatus: throws when cabinet not found")
    void testUpdateCabinetStatusNotFound() {
        given(cabinetRepository.findByVisibleNumWithLock(99)).willReturn(Optional.empty());

        CabinetStatusRequest request =
                new CabinetStatusRequest(CabinetStatus.BROKEN, LentType.PRIVATE, "Test broken");
        assertThatThrownBy(() -> adminCabinetService.updateCabinetStatus(99, request))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("forceReturn: ends lent and updates cabinet to PENDING")
    void testForceReturn() {
        Cabinet cabinet =
                Cabinet.of(1, CabinetStatus.FULL, LentType.PRIVATE, 1, "note", 1, "A", 1, 1);
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);
        LentHistory activeLent = LentHistory.of(user, cabinet, LocalDateTime.now(), null);

        given(cabinetRepository.findByVisibleNumWithLock(1)).willReturn(Optional.of(cabinet));
        given(lentRepository.findByCabinetIdAndEndedAtIsNull(cabinet.getId()))
                .willReturn(Optional.of(activeLent));

        adminCabinetService.forceReturn(1);

        assertThat(activeLent.getEndedAt()).isNotNull();
        assertThat(activeLent.getReturnMemo()).isEqualTo("관리자 강제 반납");
        assertThat(cabinet.getStatus()).isEqualTo(CabinetStatus.PENDING);
    }

    @Test
    @DisplayName("approveManualReturn: changes PENDING to AVAILABLE")
    void testApproveManualReturn() {
        Cabinet cabinet =
                Cabinet.of(1, CabinetStatus.PENDING, LentType.PRIVATE, 1, "note", 1, "A", 1, 1);
        given(cabinetRepository.findByVisibleNumWithLock(1)).willReturn(Optional.of(cabinet));

        adminCabinetService.approveManualReturn(1);

        assertThat(cabinet.getStatus()).isEqualTo(CabinetStatus.AVAILABLE);
    }
}
