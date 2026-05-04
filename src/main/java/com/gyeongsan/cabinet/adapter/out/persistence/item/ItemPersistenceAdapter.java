package com.gyeongsan.cabinet.adapter.out.persistence.item;

import com.gyeongsan.cabinet.domain.item.port.out.ItemRepositoryPort;
import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ItemPersistenceAdapter implements ItemRepositoryPort {

    private final ItemRepository itemRepository;

    @Override
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    @Override
    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    @Override
    public Optional<Item> findByName(String name) {
        return itemRepository.findByName(name);
    }

    @Override
    public Optional<Item> findByType(ItemType type) {
        return itemRepository.findByType(type);
    }

    @Override
    public Item save(Item item) {
        return itemRepository.save(item);
    }
}
