package com.gyeongsan.cabinet.item.domain;

import com.gyeongsan.cabinet.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ITEM_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public ItemHistory(LocalDateTime purchaseAt, LocalDateTime usedAt, User user, Item item) {
        this.purchaseAt = purchaseAt;
        this.usedAt = usedAt;
        this.user = user;
        this.item = item;
    }

    public void use() {
        this.usedAt = LocalDateTime.now();
    }
}
