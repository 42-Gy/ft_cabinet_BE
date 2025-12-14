package com.gyeongsan.cabinet.item.repository;

import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByType(ItemType type);
}
