package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

public record AdminDashboardResponse(
        Long totalUserCount,
        Long totalCabinetCount,
        Long activeLentCount,
        Long brokenCabinetCount,
        Long bannedUserCount) {}
