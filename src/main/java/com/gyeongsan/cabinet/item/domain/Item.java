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
    private String name; // 아이템 이름

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false)
    private ItemType type; // 아이템 종류

    @Column(name = "PRICE", nullable = false)
    private Long price; // 가격

    @Column(name = "DESCRIPTION")
    private String description; // 설명

    public Item(String name, ItemType type, Long price, String description) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.description = description;
    }
}