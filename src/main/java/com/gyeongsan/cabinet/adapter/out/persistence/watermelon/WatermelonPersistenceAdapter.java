package com.gyeongsan.cabinet.adapter.out.persistence.watermelon;

import com.gyeongsan.cabinet.domain.watermelon.domain.EnhancementResult;
import com.gyeongsan.cabinet.domain.watermelon.domain.Watermelon;
import com.gyeongsan.cabinet.domain.watermelon.domain.WatermelonEventLog;
import com.gyeongsan.cabinet.domain.watermelon.port.out.WatermelonEventLogRepositoryPort;
import com.gyeongsan.cabinet.domain.watermelon.port.out.WatermelonRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WatermelonPersistenceAdapter implements WatermelonRepositoryPort, WatermelonEventLogRepositoryPort {

    private final WatermelonJpaRepository jpaRepository;
    private final WatermelonEventLogJpaRepository logJpaRepository;

    @Override
    public Optional<Watermelon> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public Watermelon save(Watermelon watermelon) {
        WatermelonEntity entity = toEntity(watermelon);
        WatermelonEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Page<Watermelon> findAllOrderByRanking(Pageable pageable) {
        return jpaRepository.findAllOrderByRanking(pageable).map(this::toDomain);
    }

    @Override
    public long findRankByUserId(Long userId) {
        return jpaRepository.findByUserId(userId)
                .map(entity -> jpaRepository.countBetterThan(
                        entity.getHighestLevel(),
                        entity.getHighestLevelAchievedAt(),
                        entity.getTotalAttempts(),
                        entity.getUserId()))
                .orElseGet(() -> jpaRepository.count() + 1);
    }

    @Override
    public WatermelonEventLog save(WatermelonEventLog log) {
        WatermelonEventLogEntity entity = toEntity(log);
        WatermelonEventLogEntity saved = logJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<WatermelonEventLog> findAllByUserId(Long userId) {
        return logJpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private WatermelonEntity toEntity(Watermelon domain) {
        WatermelonEntity entity = jpaRepository.findByUserId(domain.getUserId())
                .orElseGet(() -> {
                    WatermelonEntity newEntity = new WatermelonEntity();
                    newEntity.setUserId(domain.getUserId());
                    return newEntity;
                });
        entity.setCurrentLevel(domain.getCurrentLevel());
        entity.setHighestLevel(domain.getHighestLevel());
        entity.setHighestLevelAchievedAt(domain.getHighestLevelAchievedAt());
        entity.setTotalAttempts(domain.getTotalAttempts());
        entity.setTotalSuccesses(domain.getTotalSuccesses());
        entity.setTotalMaintains(domain.getTotalMaintains());
        entity.setTotalDrops(domain.getTotalDrops());
        entity.setTotalDestroys(domain.getTotalDestroys());
        entity.setDropProtectionCount(domain.getDropProtectionCount());
        entity.setDestroyProtectionCount(domain.getDestroyProtectionCount());
        entity.setPremiumFertilizerCount(domain.getPremiumFertilizerCount());
        entity.setDangerousFertilizerCount(domain.getDangerousFertilizerCount());
        return entity;
    }

    private Watermelon toDomain(WatermelonEntity entity) {
        return Watermelon.builder()
                .userId(entity.getUserId())
                .currentLevel(entity.getCurrentLevel())
                .highestLevel(entity.getHighestLevel())
                .highestLevelAchievedAt(entity.getHighestLevelAchievedAt())
                .totalAttempts(entity.getTotalAttempts())
                .totalSuccesses(entity.getTotalSuccesses())
                .totalMaintains(entity.getTotalMaintains())
                .totalDrops(entity.getTotalDrops())
                .totalDestroys(entity.getTotalDestroys())
                .dropProtectionCount(entity.getDropProtectionCount())
                .destroyProtectionCount(entity.getDestroyProtectionCount())
                .premiumFertilizerCount(entity.getPremiumFertilizerCount())
                .dangerousFertilizerCount(entity.getDangerousFertilizerCount())
                .build();
    }

    private WatermelonEventLogEntity toEntity(WatermelonEventLog domain) {
        WatermelonEventLogEntity entity = new WatermelonEventLogEntity();
        entity.setUserId(domain.getUserId());
        entity.setUserName(domain.getUserName());
        entity.setBeforeLevel(domain.getBeforeLevel());
        entity.setAfterLevel(domain.getAfterLevel());
        entity.setUsedPremiumFertilizer(domain.isUsedPremiumFertilizer());
        entity.setUsedDangerousFertilizer(domain.isUsedDangerousFertilizer());
        entity.setUsedDropProtection(domain.isUsedDropProtection());
        entity.setUsedDestroyProtection(domain.isUsedDestroyProtection());
        entity.setRawOutcome(domain.getRawOutcome().name());
        entity.setFinalOutcome(domain.getFinalOutcome().name());
        entity.setCostSeeds(domain.getCostSeeds());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private WatermelonEventLog toDomain(WatermelonEventLogEntity entity) {
        return WatermelonEventLog.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .userName(entity.getUserName())
                .beforeLevel(entity.getBeforeLevel())
                .afterLevel(entity.getAfterLevel())
                .usedPremiumFertilizer(entity.isUsedPremiumFertilizer())
                .usedDangerousFertilizer(entity.isUsedDangerousFertilizer())
                .usedDropProtection(entity.isUsedDropProtection())
                .usedDestroyProtection(entity.isUsedDestroyProtection())
                .rawOutcome(EnhancementResult.valueOf(entity.getRawOutcome()))
                .finalOutcome(EnhancementResult.valueOf(entity.getFinalOutcome()))
                .costSeeds(entity.getCostSeeds())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
