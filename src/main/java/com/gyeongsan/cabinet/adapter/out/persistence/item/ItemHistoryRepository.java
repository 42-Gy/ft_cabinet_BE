package com.gyeongsan.cabinet.adapter.out.persistence.item;

import com.gyeongsan.cabinet.domain.item.model.ItemHistory;
import com.gyeongsan.cabinet.domain.item.model.ItemType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemHistoryRepository extends JpaRepository<ItemHistory, Long> {

    @Query(
            "SELECT ih FROM ItemHistory ih JOIN FETCH ih.item i "
                    + "WHERE ih.user.id = :userId AND i.type = :itemType AND ih.usedAt IS NULL "
                    + "ORDER BY ih.purchaseAt ASC")
    List<ItemHistory> findUnusedItems(
            @Param("userId") Long userId, @Param("itemType") ItemType itemType);

    @Query(
            "SELECT ih FROM ItemHistory ih JOIN FETCH ih.item "
                    + "WHERE ih.user.id = :userId AND ih.usedAt IS NULL "
                    + "ORDER BY ih.purchaseAt DESC")
    List<ItemHistory> findAllByUserIdAndUsedAtIsNull(@Param("userId") Long userId);

    int countByUserIdAndItem_TypeAndUsedAtIsNull(Long userId, ItemType itemType);

    int countByUserIdAndItem_TypeAndPurchaseAtBetween(
            Long userId, ItemType itemType, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(i.price) FROM ItemHistory ih JOIN ih.item i")
    Long sumUsedItemPrice();

    @Query("SELECT i.name, COUNT(ih) FROM ItemHistory ih JOIN ih.item i GROUP BY i.name")
    List<Object[]> findItemSales();

    @Query(
            """
                     SELECT i.name, i.type,
                            COUNT(ih),
                            SUM(CASE WHEN ih.usedAt IS NOT NULL THEN 1 ELSE 0 END)
                     FROM ItemHistory ih
                     JOIN ih.item i
                     GROUP BY i.name, i.type
                     ORDER BY i.name
                     """)
    List<Object[]> findItemUsageStats();

    void deleteAllByUserAndItemAndUsedAtIsNull(
            com.gyeongsan.cabinet.domain.user.model.User user,
            com.gyeongsan.cabinet.domain.item.model.Item item);

    @Query(
            "SELECT ih FROM ItemHistory ih JOIN FETCH ih.item WHERE ih.user.id = :userId ORDER BY ih.purchaseAt DESC")
    List<ItemHistory> findAllByUserIdOrderByPurchaseAtDesc(@Param("userId") Long userId);
}
