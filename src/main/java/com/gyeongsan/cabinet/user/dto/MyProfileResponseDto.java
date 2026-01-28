package com.gyeongsan.cabinet.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.gyeongsan.cabinet.user.domain.UserRole;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyProfileResponseDto {

    private Long userId;
    private String name;
    private String email;
    private Long coin;

    private UserRole role;

    private Integer penaltyDays;
    private Integer monthlyLogtime;

    private Long lentCabinetId;
    private Integer visibleNum;
    private String section;
    private Boolean autoExtensionEnabled;

    private String previousPassword;

    private String lentStartedAt;

    private String expiredAt;

    private Integer overdueDays;

    private List<MyItemDto> myItems;
    private List<CoinHistoryDto> coinHistories;
    private List<ItemHistoryDto> itemHistories;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyItemDto {
        private Long itemHistoryId;
        private String itemName;
        private String itemType;
        private LocalDateTime purchaseAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CoinHistoryDto {
        private String date;
        private Long amount;
        private String type;
        private String reason;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemHistoryDto {
        private String date;
        private String itemName;
        private String itemType;
        private String status;
        private String usedAt;
    }
}
