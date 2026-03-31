package com.haushaltsplaner.dto;

import com.haushaltsplaner.domain.TaskFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {
    
    @NotBlank(message = "Task name is required")
    private String name;
    
    @NotNull(message = "Frequency is required")
    private TaskFrequency frequency;
    
    private LocalDate dueDate;
    private LocalDate completionPeriodStart;
    private LocalDate completionPeriodEnd;
    private Long assignedUserId;
    private Integer points;
}
