package com.gyeongsan.cabinet.item.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false)
    private ItemType type;

    @Column(name = "PRICE", nullable = false)
    private Long price;

    @Column(name = "DESCRIPTION")
    private String description;

    public Item(String name, ItemType type, Long price, String description) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.description = description;
    }
}
