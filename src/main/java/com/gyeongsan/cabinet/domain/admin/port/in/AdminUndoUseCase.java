package com.gyeongsan.cabinet.domain.admin.port.in;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoResponse;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;

public interface AdminUndoUseCase {

    /** 사물함 일괄 변경(CABINET_BULK_STATUS_UPDATE)을 되돌린다. 그 사이 바뀐 것이 하나라도 있으면 아무것도 바꾸지 않고 전체 거부한다. */
    UndoResponse undoBulkStatusUpdate(String batchId, UndoRequest request, AdminActor actor);
}
