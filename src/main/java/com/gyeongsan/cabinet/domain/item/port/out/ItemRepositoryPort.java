package com.gyeongsan.cabinet.domain.item.port.out;

import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemType;

import java.util.List;
import java.util.Optional;

public interface ItemRepositoryPort {

    List<Item> findAll();

    Optional<Item> findById(Long id);

    Optional<Item> findByName(String name);

    Optional<Item> findByType(ItemType type);

    Item save(Item item);
}
