package com.gyeongsan.cabinet.adapter.out.persistence.admin;

import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import java.time.LocalDateTime;

/** 목록 조회용 JPQL 프로젝션. 항목 수와 Undo 여부는 서브쿼리로 한 번에 가져온다. */
interface AdminActionLogSummaryView {

    String getBatchId();

    AdminActionType getActionType();

    Long getActorId();

    String getActorName();

    String getReason();

    LocalDateTime getCreatedAt();

    String getUndoOfBatchId();

    Long getItemCount();

    String getUndoneByBatchId();
}
