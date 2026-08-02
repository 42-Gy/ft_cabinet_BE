package com.gyeongsan.cabinet.adapter.out.persistence.cabinet;

import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CabinetRepository extends JpaRepository<Cabinet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cabinet c WHERE c.id = :id")
    Optional<Cabinet> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cabinet c WHERE c.visibleNum = :visibleNum")
    Optional<Cabinet> findByVisibleNumWithLock(@Param("visibleNum") Integer visibleNum);

    Optional<Cabinet> findByVisibleNum(Integer visibleNum);

    List<Cabinet> findAllByFloor(Integer floor);

    List<Cabinet> findAllByStatus(CabinetStatus status);

    long countByStatus(CabinetStatus status);

    interface FloorStatProjection {
        Integer getFloor();

        Long getTotal();

        Long getUsed();

        Long getAvailable();

        Long getOverdue();

        Long getBroken();

        Long getPending();

        Long getDisabled();
    }

    @Query(
            value =
                    """
            SELECT
                c.floor AS floor,
                COUNT(*) AS total,
                SUM(CASE WHEN c.status = 'FULL' THEN 1 ELSE 0 END) AS used,
                SUM(CASE WHEN c.status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available,
                SUM(CASE WHEN c.status = 'OVERDUE' THEN 1 ELSE 0 END) AS overdue,
                SUM(CASE WHEN c.status = 'BROKEN' THEN 1 ELSE 0 END) AS broken,
                SUM(CASE WHEN c.status = 'PENDING' THEN 1 ELSE 0 END) AS pending,
                SUM(CASE WHEN c.status = 'DISABLED' THEN 1 ELSE 0 END) AS disabled
            FROM CABINET c
            GROUP BY c.floor
            ORDER BY c.floor
            """,
            nativeQuery = true)
    List<FloorStatProjection> findFloorStatistics();
}
