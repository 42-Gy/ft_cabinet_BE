package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.user.model.User;

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
