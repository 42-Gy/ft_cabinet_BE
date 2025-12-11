package com.gyeongsan.cabinet.cabinet.repository;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CabinetRepository extends JpaRepository<Cabinet, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cabinet c WHERE c.id = :id")
    Optional<Cabinet> findByIdWithLock(@Param("id") Long id);
}