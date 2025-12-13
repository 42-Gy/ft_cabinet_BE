package com.gyeongsan.cabinet.admin.dto;

public record CoinProvideRequest(
        Long amount,
        String reason
) {}
