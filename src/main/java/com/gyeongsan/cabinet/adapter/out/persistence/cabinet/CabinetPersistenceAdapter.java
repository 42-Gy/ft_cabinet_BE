package com.gyeongsan.cabinet.adapter.out.persistence.cabinet;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository.FloorStatProjection;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CabinetPersistenceAdapter implements CabinetRepositoryPort {

    private final CabinetRepository cabinetRepository;

    @Override
    public Optional<Cabinet> findById(Long id) {
        return cabinetRepository.findById(id);
    }

    @Override
    public Optional<Cabinet> findByIdWithLock(Long id) {
        return cabinetRepository.findByIdWithLock(id);
    }

    @Override
    public Optional<Cabinet> findByVisibleNumWithLock(Integer visibleNum) {
        return cabinetRepository.findByVisibleNumWithLock(visibleNum);
    }

    @Override
    public List<Cabinet> findAll() {
        return cabinetRepository.findAll();
    }

    @Override
    public List<Cabinet> findAllById(List<Long> ids) {
        return cabinetRepository.findAllById(ids);
    }

    @Override
    public List<Cabinet> findAllByFloor(Integer floor) {
        return cabinetRepository.findAllByFloor(floor);
    }

    @Override
    public List<Cabinet> findAllByStatus(CabinetStatus status) {
        return cabinetRepository.findAllByStatus(status);
    }

    @Override
    public long count() {
        return cabinetRepository.count();
    }

    @Override
    public long countByStatus(CabinetStatus status) {
        return cabinetRepository.countByStatus(status);
    }

    @Override
    public List<FloorStatProjection> findFloorStatistics() {
        return cabinetRepository.findFloorStatistics();
    }
}
