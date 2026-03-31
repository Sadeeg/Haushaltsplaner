package com.haushaltsplaner.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntry {
    private Long userId;
    private String displayName;
    private Integer totalPoints;
    private Integer completedTasks;
    private Integer skippedTasks;
}
