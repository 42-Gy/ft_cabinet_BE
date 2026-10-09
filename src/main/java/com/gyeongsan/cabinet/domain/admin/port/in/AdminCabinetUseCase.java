package com.gyeongsan.cabinet.domain.admin.port.in;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCabinetUseCase {
    Page<CabinetHistoryResponse> getCabinetHistory(Integer visibleNum, Pageable pageable);

    void updateCabinetStatus(Integer visibleNum, CabinetStatusRequest request);

    void forceReturn(Integer visibleNum);

    List<CabinetPendingResponseDto> getPendingCabinets();

    Page<ReturnPhotoResponseDto> getReturnPhotos(Pageable pageable);

    void approveManualReturn(Integer visibleNum);

    CabinetDetailResponse getCabinetDetail(Integer visibleNum);

    List<BrokenCabinetResponse> getBrokenCabinets();

    void bulkUpdateCabinetStatus(BulkStatusUpdateRequest request);
}
