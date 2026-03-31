package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.TaskTemplate;
import com.haushaltsplaner.dto.TaskTemplateDto;
import com.haushaltsplaner.repository.TaskTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskTemplateService {

    private final TaskTemplateRepository taskTemplateRepository;

    @Transactional(readOnly = true)
    public List<TaskTemplateDto> getTemplatesForHousehold(Long householdId) {
        return taskTemplateRepository.findByHouseholdId(householdId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskTemplateDto createTemplate(TaskTemplateDto dto, Long householdId) {
        TaskTemplate template = TaskTemplate.builder()
                .name(dto.getName())
                .frequency(dto.getFrequency())
                .defaultPoints(dto.getDefaultPoints() != null ? dto.getDefaultPoints() : 1)
                .completionPeriodDays(dto.getCompletionPeriodDays())
                .build();

        return toDto(taskTemplateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        taskTemplateRepository.deleteById(templateId);
    }

    private TaskTemplateDto toDto(TaskTemplate template) {
        return TaskTemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .frequency(template.getFrequency())
                .defaultPoints(template.getDefaultPoints())
                .completionPeriodDays(template.getCompletionPeriodDays())
                .build();
    }
}
