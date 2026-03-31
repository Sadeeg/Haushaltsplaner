package com.haushaltsplaner.dto;

import com.haushaltsplaner.domain.TaskFrequency;
import com.haushaltsplaner.domain.TaskStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDto {
    private Long id;
    private String name;
    private TaskFrequency frequency;
    private LocalDate dueDate;
    private LocalDate completionPeriodStart;
    private LocalDate completionPeriodEnd;
    private TaskStatus status;
    private Long assignedUserId;
    private String assignedUserName;
    private Integer points;
    private LocalDateTime completedAt;
}
