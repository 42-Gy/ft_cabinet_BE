package com.gyeongsan.cabinet.adapter.out.persistence.admin;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminActionLogJpaRepository extends JpaRepository<AdminActionLogEntity, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<AdminActionLogEntity> findByBatchId(String batchId);

    @Query("SELECT l.batchId FROM AdminActionLogEntity l WHERE l.undoOfBatchId = :batchId")
    Optional<String> findUndoBatchIdOf(@Param("batchId") String batchId);

    @Query(
            value =
                    "SELECT l.batchId AS batchId, l.actionType AS actionType, l.actorId AS actorId,"
                            + " l.actorName AS actorName, l.reason AS reason, l.createdAt AS createdAt,"
                            + " l.undoOfBatchId AS undoOfBatchId,"
                            + " (SELECT COUNT(i) FROM AdminActionLogItemEntity i WHERE i.log = l)"
                            + " AS itemCount,"
                            + " (SELECT u.batchId FROM AdminActionLogEntity u"
                            + " WHERE u.undoOfBatchId = l.batchId) AS undoneByBatchId"
                            + " FROM AdminActionLogEntity l ORDER BY l.createdAt DESC, l.id DESC",
            countQuery = "SELECT COUNT(l) FROM AdminActionLogEntity l")
    Page<AdminActionLogSummaryView> findSummaries(Pageable pageable);
}
