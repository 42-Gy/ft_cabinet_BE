package com.gyeongsan.cabinet.domain.cabinet.port.out;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository.FloorStatProjection;

import java.util.List;
import java.util.Optional;

public interface CabinetRepositoryPort {

    Optional<Cabinet> findById(Long id);

    Optional<Cabinet> findByIdWithLock(Long id);

    Optional<Cabinet> findByVisibleNumWithLock(Integer visibleNum);

    List<Cabinet> findAll();

    List<Cabinet> findAllById(List<Long> ids);

    List<Cabinet> findAllByFloor(Integer floor);

    List<Cabinet> findAllByStatus(CabinetStatus status);

    long count();

    long countByStatus(CabinetStatus status);

    List<FloorStatProjection> findFloorStatistics();
}
