package com.gyeongsan.cabinet.adapter.out.persistence.watermelon;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatermelonEventLogJpaRepository
        extends JpaRepository<WatermelonEventLogEntity, Long> {
    List<WatermelonEventLogEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<WatermelonEventLogEntity> findAllByOrderByCreatedAtDesc();
}
