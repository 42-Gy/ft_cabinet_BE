package com.gyeongsan.cabinet.adapter.out.persistence.watermelon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "WATERMELON", indexes = {
    @Index(name = "idx_watermelon_ranking", columnList = "highest_level DESC, highest_level_achieved_at ASC, total_attempts ASC")
})
@Getter
@Setter
@NoArgsConstructor
public class WatermelonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_ID", unique = true, nullable = false)
    private Long userId;

    @Column(name = "CURRENT_LEVEL", nullable = false)
    private int currentLevel;

    @Column(name = "HIGHEST_LEVEL", nullable = false)
    private int highestLevel;

    @Column(name = "HIGHEST_LEVEL_ACHIEVED_AT", nullable = false)
    private LocalDateTime highestLevelAchievedAt;

    @Column(name = "TOTAL_ATTEMPTS", nullable = false)
    private int totalAttempts;

    @Column(name = "TOTAL_SUCCESSES", nullable = false)
    private int totalSuccesses;

    @Column(name = "TOTAL_MAINTAINS", nullable = false)
    private int totalMaintains;

    @Column(name = "TOTAL_DROPS", nullable = false)
    private int totalDrops;

    @Column(name = "TOTAL_DESTROYS", nullable = false)
    private int totalDestroys;

    @Column(name = "DROP_PROTECTION_COUNT", nullable = false)
    private int dropProtectionCount;

    @Column(name = "DESTROY_PROTECTION_COUNT", nullable = false)
    private int destroyProtectionCount;

    @Column(name = "PREMIUM_FERTILIZER_COUNT", nullable = false)
    private int premiumFertilizerCount;

    @Column(name = "DANGEROUS_FERTILIZER_COUNT", nullable = false)
    private int dangerousFertilizerCount;
}
