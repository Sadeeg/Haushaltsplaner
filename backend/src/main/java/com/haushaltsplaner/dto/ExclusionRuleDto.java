package com.haushaltsplaner.dto;

import com.haushaltsplaner.domain.ExclusionType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExclusionRuleDto {
    private Long id;
    private Long taskATemplateId;
    private String taskAName;
    private Long taskBTemplateId;
    private String taskBName;
    private ExclusionType ruleType;
}
