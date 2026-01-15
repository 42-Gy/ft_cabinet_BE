package com.gyeongsan.cabinet.admin.dto;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.user.domain.User;

public record CabinetDetailResponse(
        Long cabinetId,
        Integer visibleNum,
        CabinetStatus status,
        String lentType,
        Integer maxUser,
        String section,
        String location,
        String title,
        String currentUserName,
        Long currentUserId) {
    public static CabinetDetailResponse of(Cabinet cabinet, LentHistory activeLent) {
        String userName = null;
        Long userId = null;

        if (activeLent != null) {
            User user = activeLent.getUser();
            userName = user.getName();
            userId = user.getId();
        }

        return new CabinetDetailResponse(
                cabinet.getId(),
                cabinet.getVisibleNum(),
                cabinet.getStatus(),
                cabinet.getLentType().name(),
                cabinet.getMaxUser(),
                cabinet.getSection(),
                cabinet.getFloor() + "F " + cabinet.getSection(),
                "",
                userName,
                userId);
    }
}
