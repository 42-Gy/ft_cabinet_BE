package com.gyeongsan.cabinet.adapter.out.persistence.lent;

import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LentPersistenceAdapter implements LentRepositoryPort {

    private final LentRepository lentRepository;

    @Override
    public Optional<LentHistory> findByUserIdAndEndedAtIsNull(Long userId) {
        return lentRepository.findByUserIdAndEndedAtIsNull(userId);
    }

    @Override
    public Optional<LentHistory> findByCabinetIdAndEndedAtIsNull(Long cabinetId) {
        return lentRepository.findByCabinetIdAndEndedAtIsNull(cabinetId);
    }

    @Override
    public Optional<LentHistory> findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(Long cabinetId) {
        return lentRepository.findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(cabinetId);
    }

    @Override
    public long countByEndedAtIsNull() {
        return lentRepository.countByEndedAtIsNull();
    }

    @Override
    public List<LentHistory> findAllOverdueLentHistories(LocalDateTime now) {
        return lentRepository.findAllOverdueLentHistories(now);
    }

    @Override
    public List<LentHistory> findAllActiveLentByCabinetIds(List<Long> cabinetIds) {
        return lentRepository.findAllActiveLentByCabinetIds(cabinetIds);
    }

    @Override
    public List<LentHistory> findAllActiveLentsByExpiredAtBetween(LocalDateTime start, LocalDateTime end) {
        return lentRepository.findAllActiveLentsByExpiredAtBetween(start, end);
    }

    @Override
    public List<LentHistory> findAllActiveLents() {
        return lentRepository.findAllActiveLents();
    }

    @Override
    public List<LentHistory> findAllLatestLentForPendingCabinets() {
        return lentRepository.findAllLatestLentForPendingCabinets();
    }

    @Override
    public List<LentHistory> findRecentExpiredActiveLents(LocalDateTime start, LocalDateTime end) {
        return lentRepository.findRecentExpiredActiveLents(start, end);
    }

    @Override
    public Page<LentHistory> findHistoryByCabinet(Integer visibleNum, Pageable pageable) {
        return lentRepository.findHistoryByCabinet(visibleNum, pageable);
    }

    @Override
    public Page<LentHistory> findAllReturnedWithPhoto(Pageable pageable) {
        return lentRepository.findAllReturnedWithPhoto(pageable);
    }

    @Override
    public long countLentsStartedAtBetween(LocalDateTime start, LocalDateTime end) {
        return lentRepository.countLentsStartedAtBetween(start, end);
    }

    @Override
    public long countLentsEndedAtBetween(LocalDateTime start, LocalDateTime end) {
        return lentRepository.countLentsEndedAtBetween(start, end);
    }

    @Override
    public LentHistory save(LentHistory lentHistory) {
        return lentRepository.save(lentHistory);
    }
}
