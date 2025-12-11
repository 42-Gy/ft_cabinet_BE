package com.gyeongsan.cabinet.item.domain;

import com.gyeongsan.cabinet.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener; // 필요한 경우 추가

import java.time.LocalDateTime;

@Entity
@Table(name = "ITEM_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// @EntityListeners(AuditingEntityListener.class) // Auditing을 사용한다면 추가
public class ItemHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PURCHASE_AT", nullable = false)
    private LocalDateTime purchaseAt;

    @Column(name = "USED_AT")
    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITEM_ID", nullable = false)
    private Item item;

    // --- 기본 생성자 (비즈니스 로직용) ---
    public ItemHistory(LocalDateTime purchaseAt, LocalDateTime usedAt, User user, Item item) {
        this.purchaseAt = purchaseAt;
        this.usedAt = usedAt;
        this.user = user;
        this.item = item;
    }

    // 👇 [핵심 추가] 테스트/DB 스크립트 주입을 위한 ID 기반 생성자
    // 이 생성자는 실제 EntityManager를 사용할 수 없으므로, Lombok 생성자에 맞게 수정합니다.
    // Lombok을 사용하지 않는 경우, 필드를 직접 받는 생성자를 추가해야 합니다.

    // 이 문제를 해결하는 가장 간단한 방법은 ItemHistoryRepository에서 Native Query를 통해 DTO를 받는 것입니다.
    // 하지만 현재 엔티티 구조를 유지하기 위해, @Sql 스크립트를 우회하는 생성자를 추가하지 않고,
    // LentFacadeService에서 ItemHistoryRepository의 쿼리 문제를 우회하는 방법을 선택하겠습니다.

    // **주: @Sql 삽입 문제로 인해, 이 파일은 수정 없이 ItemHistoryRepository의 쿼리만 수정하는 것이 최선입니다.**

    // --- 비즈니스 로직 ---

    // 아이템 사용 처리
    public void use() {
        this.usedAt = LocalDateTime.now();
    }
}