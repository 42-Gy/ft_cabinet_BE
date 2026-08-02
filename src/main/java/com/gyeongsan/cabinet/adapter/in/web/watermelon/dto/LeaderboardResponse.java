package com.gyeongsan.cabinet.adapter.in.web.watermelon.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaderboardResponse {
    private final Long userId;
    private final String username;
    private final int highestLevel;
    private final LocalDateTime highestLevelAchievedAt;
    private final int totalAttempts;
}
