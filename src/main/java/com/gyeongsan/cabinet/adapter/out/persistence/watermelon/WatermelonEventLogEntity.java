package com.gyeongsan.cabinet.adapter.out.persistence.watermelon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "WATERMELON_EVENT_LOG", indexes = {
    @Index(name = "idx_wm_log_user_id", columnList = "USER_ID")
})
@Getter
@Setter
@NoArgsConstructor
public class WatermelonEventLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "BEFORE_LEVEL", nullable = false)
    private int beforeLevel;

    @Column(name = "AFTER_LEVEL", nullable = false)
    private int afterLevel;

    @Column(name = "USED_PREMIUM_FERTILIZER", nullable = false)
    private boolean usedPremiumFertilizer;

    @Column(name = "USED_DANGEROUS_FERTILIZER", nullable = false)
    private boolean usedDangerousFertilizer;

    @Column(name = "USED_DROP_PROTECTION", nullable = false)
    private boolean usedDropProtection;

    @Column(name = "USED_DESTROY_PROTECTION", nullable = false)
    private boolean usedDestroyProtection;

    @Column(name = "RAW_OUTCOME", nullable = false, length = 20)
    private String rawOutcome;

    @Column(name = "FINAL_OUTCOME", nullable = false, length = 20)
    private String finalOutcome;

    @Column(name = "COST_SEEDS", nullable = false)
    private int costSeeds;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}
