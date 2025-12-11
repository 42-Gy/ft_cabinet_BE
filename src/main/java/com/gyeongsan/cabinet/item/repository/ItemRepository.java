package com.gyeongsan.cabinet.item.repository;

import com.gyeongsan.cabinet.item.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

// 아이템 정보(가격, 설명 등)를 DB에서 가져오는 창고
public interface ItemRepository extends JpaRepository<Item, Long> {
    // 지금은 기본 기능(findById 등)만 있으면 돼서 내용은 비워둬도 됩니다!
}