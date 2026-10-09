package com.gyeongsan.cabinet.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogDetailResponse;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogSummaryResponse;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogItem;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogSummary;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;
import com.gyeongsan.cabinet.domain.admin.port.out.AdminActionLogPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AdminActionLogQueryServiceTest {

    @Mock private AdminActionLogPort adminActionLogPort;
    @InjectMocks private AdminActionLogQueryService service;

    @Test
    @DisplayName("목록은 페이지 크기를 100으로 제한하고, 요청의 정렬 값은 무시한다")
    void list_capsPageSizeAndIgnoresSort() {
        AdminActionLogSummary summary =
                new AdminActionLogSummary(
                        "b-1",
                        AdminActionType.CABINET_BULK_STATUS_UPDATE,
                        1L,
                        "admin01",
                        "월말",
                        LocalDateTime.of(2026, 10, 5, 12, 0),
                        3,
                        null,
                        "u-1");
        given(adminActionLogPort.findSummaries(any())).willReturn(new PageImpl<>(List.of(summary)));

        Page<AdminActionLogSummaryResponse> page =
                service.getActionLogs(PageRequest.of(2, 5000, Sort.by("actorName")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(adminActionLogPort).findSummaries(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
        assertThat(page.getContent())
                .singleElement()
                .satisfies(
                        r -> {
                            assertThat(r.batchId()).isEqualTo("b-1");
                            assertThat(r.itemCount()).isEqualTo(3);
                            assertThat(r.undoneByBatchId()).isEqualTo("u-1");
                        });
    }

    @Test
    @DisplayName("상세는 항목의 변경 전/후 값과 Undo 여부를 담는다")
    void detail_includesItemsAndUndoneBy() {
        AdminActionLog log =
                new AdminActionLog(
                        "b-1",
                        AdminActionType.CABINET_BULK_STATUS_UPDATE,
                        new AdminActor(1L, "admin01"),
                        "월말",
                        Map.of("status", "AVAILABLE"),
                        LocalDateTime.of(2026, 10, 5, 12, 0),
                        List.of(
                                new AdminActionLogItem(
                                        AdminActionTargetType.CABINET,
                                        7L,
                                        "101",
                                        Map.of("status", "FULL"),
                                        Map.of("status", "AVAILABLE"))));
        given(adminActionLogPort.findByBatchId("b-1")).willReturn(Optional.of(log));
        given(adminActionLogPort.findUndoBatchIdOf("b-1")).willReturn(Optional.of("u-1"));

        AdminActionLogDetailResponse detail = service.getActionLog("b-1");

        assertThat(detail.undoneByBatchId()).isEqualTo("u-1");
        assertThat(detail.actorName()).isEqualTo("admin01");
        assertThat(detail.items())
                .singleElement()
                .satisfies(
                        i -> {
                            assertThat(i.targetId()).isEqualTo(7L);
                            assertThat(i.before()).containsEntry("status", "FULL");
                            assertThat(i.after()).containsEntry("status", "AVAILABLE");
                        });
    }

    @Test
    @DisplayName("없는 batchId 는 404 로 처리한다")
    void detail_notFound() {
        given(adminActionLogPort.findByBatchId("none")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActionLog("none"))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.ADMIN_ACTION_LOG_NOT_FOUND));
    }
}
