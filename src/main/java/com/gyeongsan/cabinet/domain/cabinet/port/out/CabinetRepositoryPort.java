package com.gyeongsan.cabinet.domain.cabinet.port.out;

import com.gyeongsan.cabinet.adapter.out.persistence.cabinet.CabinetRepository.FloorStatProjection;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import java.util.List;
import java.util.Optional;

public interface CabinetRepositoryPort {

    Optional<Cabinet> findById(Long id);

    Optional<Cabinet> findByIdWithLock(Long id);

    Optional<Cabinet> findByVisibleNum(Integer visibleNum);

    Optional<Cabinet> findByVisibleNumWithLock(Integer visibleNum);

    List<Cabinet> findAll();

    List<Cabinet> findAllById(List<Long> ids);

    List<Cabinet> findAllByFloor(Integer floor);

    List<Cabinet> findAllByStatus(CabinetStatus status);

    long count();

    long countByStatus(CabinetStatus status);

    List<FloorStatProjection> findFloorStatistics();
}
