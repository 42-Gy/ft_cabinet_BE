package com.gyeongsan.cabinet.adapter.out.persistence.watermelon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatermelonEventLogJpaRepository extends JpaRepository<WatermelonEventLogEntity, Long> {
    List<WatermelonEventLogEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
