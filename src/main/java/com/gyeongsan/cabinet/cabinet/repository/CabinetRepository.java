package com.gyeongsan.cabinet.cabinet.repository;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List; // 👈 추가
import java.util.Optional;

public interface CabinetRepository extends JpaRepository<Cabinet, Long> {

    // 1. [기존] 비관적 락을 걸고 사물함 조회 (대여/반납 시 동시성 제어)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cabinet c WHERE c.id = :id")
    Optional<Cabinet> findByIdWithLock(@Param("id") Long id);

    // 2. [추가] 층수 기반으로 모든 사물함 조회 (목록 API에서 사용)
    List<Cabinet> findAllByFloor(Integer floor);
}