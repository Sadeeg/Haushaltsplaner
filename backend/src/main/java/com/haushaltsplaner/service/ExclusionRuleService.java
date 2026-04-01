package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.ExclusionRule;
import com.haushaltsplaner.domain.ExclusionType;
import com.haushaltsplaner.domain.Household;
import com.haushaltsplaner.domain.TaskTemplate;
import com.haushaltsplaner.dto.ExclusionRuleDto;
import com.haushaltsplaner.repository.ExclusionRuleRepository;
import com.haushaltsplaner.repository.HouseholdRepository;
import com.haushaltsplaner.repository.TaskTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExclusionRuleService {

    private final ExclusionRuleRepository exclusionRuleRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final HouseholdRepository householdRepository;

    @Transactional(readOnly = true)
    public List<ExclusionRuleDto> getRulesForHousehold(Long householdId) {
        return exclusionRuleRepository.findByHouseholdId(householdId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExclusionRuleDto createRule(ExclusionRuleDto dto, Long householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found"));

        TaskTemplate taskA = taskTemplateRepository.findById(dto.getTaskATemplateId())
                .orElseThrow(() -> new RuntimeException("Task A not found"));

        TaskTemplate taskB = taskTemplateRepository.findById(dto.getTaskBTemplateId())
                .orElseThrow(() -> new RuntimeException("Task B not found"));

        ExclusionRule rule = ExclusionRule.builder()
                .household(household)
                .taskA(taskA)
                .taskB(taskB)
                .ruleType(dto.getRuleType() != null ? dto.getRuleType() : ExclusionType.MUTUAL)
                .build();

        return toDto(exclusionRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        exclusionRuleRepository.deleteById(ruleId);
    }

    private ExclusionRuleDto toDto(ExclusionRule rule) {
        return ExclusionRuleDto.builder()
                .id(rule.getId())
                .taskATemplateId(rule.getTaskA() != null ? rule.getTaskA().getId() : null)
                .taskAName(rule.getTaskA() != null ? rule.getTaskA().getName() : null)
                .taskBTemplateId(rule.getTaskB() != null ? rule.getTaskB().getId() : null)
                .taskBName(rule.getTaskB() != null ? rule.getTaskB().getName() : null)
                .ruleType(rule.getRuleType())
                .build();
    }
}
