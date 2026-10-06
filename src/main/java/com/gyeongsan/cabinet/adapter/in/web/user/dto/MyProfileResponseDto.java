package com.gyeongsan.cabinet.adapter.in.web.user.dto;

import com.gyeongsan.cabinet.domain.user.model.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private boolean isPisciner;

    private Integer penaltyDays;
    private Integer monthlyLogtime;

    private Long lentCabinetId;
    private Integer visibleNum;
    private String section;
    private Boolean autoExtensionEnabled;

    private String previousPassword;

    private String lentStartedAt;

    /** 내가 예약 중인 사물함 번호. 예약이 없으면 null. */
    private Integer reservedVisibleNum;

    /** 내 예약이 만료되기까지 남은 초. 예약이 없으면 null. */
    private Long reservationRemainingSeconds;

    /** 표시용 만료 시각("MM월 dd일 HH:mm", 연도 없음). 새 화면은 {@link #expiredAtIso}를 쓴다. */
    private String expiredAt;

    /** 만료 시각(ISO-8601). 대여 중이 아니면 null. */
    private LocalDateTime expiredAtIso;

    /** 만료일까지 남은 일수(달력 기준). 0은 만료일 당일, 음수는 만료일이 지난 일수. 대여 중이 아니면 null. */
    private Integer daysRemaining;

    /** 현재 연체인지. 반납 시 패널티 부과와 같은 판정이다. 대여 중이 아니면 null. */
    private Boolean overdue;

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
