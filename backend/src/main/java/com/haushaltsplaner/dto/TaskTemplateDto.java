package com.haushaltsplaner.dto;

import com.haushaltsplaner.domain.TaskFrequency;
import lombok.*;

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
}
