package com.gyeongsan.cabinet.item.repository;

import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ItemHistoryRepository extends JpaRepository<ItemHistory, Long> {

    // 원래의 JPA 친화적 시그니처로 복구
    @Query("SELECT ih FROM ItemHistory ih JOIN FETCH ih.item i " +
            "WHERE ih.user.id = :userId AND i.type = :itemType AND ih.usedAt IS NULL " +
            "ORDER BY ih.purchaseAt ASC")
    // 👇 [복구] ItemType enum을 인자로 받도록 되돌립니다.
    List<ItemHistory> findUnusedItems(@Param("userId") Long userId, @Param("itemType") ItemType itemType);
}