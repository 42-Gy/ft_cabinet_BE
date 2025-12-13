package com.gyeongsan.cabinet.admin.dto;

public record AdminDashboardResponse(
        Long totalUserCount,
        Long totalCabinetCount,
        Long activeLentCount,
        Long brokenCabinetCount,
        Long bannedUserCount
) {}