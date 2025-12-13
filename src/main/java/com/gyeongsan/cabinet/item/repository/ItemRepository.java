package com.gyeongsan.cabinet.item.repository;

import com.gyeongsan.cabinet.item.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
