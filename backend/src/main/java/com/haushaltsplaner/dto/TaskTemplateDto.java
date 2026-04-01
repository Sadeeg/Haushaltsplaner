package com.haushaltsplaner.dto;

import com.haushaltsplaner.domain.TaskFrequency;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTemplateDto {
    private Long id;
    private String name;
    private TaskFrequency frequency;
    private Integer defaultPoints;
    private Integer completionPeriodDays;
    private Set<Long> assignedUserIds;
}
