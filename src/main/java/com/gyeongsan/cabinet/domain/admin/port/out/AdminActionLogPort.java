package com.gyeongsan.cabinet.domain.admin.port.out;

import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogSummary;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminActionLogPort {

    /** 호출한 트랜잭션에 참여해서 저장한다. 로그 저장이 실패하면 호출한 작업도 함께 롤백되어야 한다. */
    void save(AdminActionLog log);

    /** 항목(변경 전/후 값)까지 포함해서 읽는다. */
    Optional<AdminActionLog> findByBatchId(String batchId);

    /** 이 작업을 되돌린 Undo 기록의 batchId. 아직 되돌려지지 않았으면 비어 있다. */
    Optional<String> findUndoBatchIdOf(String originalBatchId);

    /** 최신순 목록. 정렬은 항상 발생 시각 내림차순으로 고정한다. */
    Page<AdminActionLogSummary> findSummaries(Pageable pageable);
}
