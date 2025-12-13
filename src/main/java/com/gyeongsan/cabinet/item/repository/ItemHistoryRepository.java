package com.gyeongsan.cabinet.item.repository;

import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ItemHistoryRepository extends JpaRepository<ItemHistory, Long> {

    @Query("SELECT ih FROM ItemHistory ih JOIN FETCH ih.item i " +
            "WHERE ih.user.id = :userId AND i.type = :itemType AND ih.usedAt IS NULL " +
            "ORDER BY ih.purchaseAt ASC")
    List<ItemHistory> findUnusedItems(@Param("userId") Long userId, @Param("itemType") ItemType itemType);

    @Query("SELECT ih FROM ItemHistory ih JOIN FETCH ih.item " +
            "WHERE ih.user.id = :userId AND ih.usedAt IS NULL " +
            "ORDER BY ih.purchaseAt DESC")
    List<ItemHistory> findAllByUserIdAndUsedAtIsNull(@Param("userId") Long userId);
}
