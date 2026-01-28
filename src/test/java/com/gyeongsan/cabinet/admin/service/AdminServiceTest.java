package com.gyeongsan.cabinet.admin.service;

import com.gyeongsan.cabinet.admin.dto.BulkStatusUpdateRequest;
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.domain.LentType;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private CabinetRepository cabinetRepository;

    @Mock
    private LentRepository lentRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("일괄 상태 변경 시 대여 중인 사물함이 있다면 반납 처리되어야 한다")
    void bulkUpdateCabinetStatus_withActiveLent() {

        Long cabinetId = 1L;
        List<Long> cabinetIds = List.of(cabinetId);
        CabinetStatus targetStatus = CabinetStatus.AVAILABLE;
        BulkStatusUpdateRequest request = new BulkStatusUpdateRequest(cabinetIds, targetStatus, null);

        Cabinet cabinet = mock(Cabinet.class);
        given(cabinetRepository.findAllById(cabinetIds)).willReturn(List.of(cabinet));

        LentHistory activeLent = mock(LentHistory.class);
        given(activeLent.getCabinet()).willReturn(cabinet);
        given(lentRepository.findAllActiveLentByCabinetIds(cabinetIds)).willReturn(List.of(activeLent));

        adminService.bulkUpdateCabinetStatus(request);

        then(activeLent).should(times(1)).endLent(any(LocalDateTime.class));
        then(cabinet).should(times(1)).updateStatus(targetStatus);
    }

    @Test
    @DisplayName("일괄 상태 변경 시 대여 중인 사물함이 없다면 상태만 변경된다")
    void bulkUpdateCabinetStatus_noActiveLent() {

        Long cabinetId = 2L;
        List<Long> cabinetIds = List.of(cabinetId);
        CabinetStatus targetStatus = CabinetStatus.BROKEN;
        String statusNote = "Broken";
        BulkStatusUpdateRequest request = new BulkStatusUpdateRequest(cabinetIds, targetStatus, statusNote);

        Cabinet cabinet = mock(Cabinet.class);
        given(cabinetRepository.findAllById(cabinetIds)).willReturn(List.of(cabinet));

        given(lentRepository.findAllActiveLentByCabinetIds(cabinetIds)).willReturn(Collections.emptyList());

        adminService.bulkUpdateCabinetStatus(request);

        then(cabinet).should(times(1)).updateStatus(targetStatus);
        then(cabinet).should(times(1)).updateStatusNote(statusNote);
    }
}
