package com.gyeongsan.cabinet.adapter.in.web.watermelon.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LeaderboardResponse {
    private final Long userId;
    private final String username;
    private final int highestLevel;
    private final LocalDateTime highestLevelAchievedAt;
    private final int totalAttempts;
}
