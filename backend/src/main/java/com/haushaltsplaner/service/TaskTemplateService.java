package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.Household;
import com.haushaltsplaner.domain.Task;
import com.haushaltsplaner.domain.TaskTemplate;
import com.haushaltsplaner.domain.User;
import com.haushaltsplaner.dto.TaskTemplateDto;
import com.haushaltsplaner.repository.HouseholdRepository;
import com.haushaltsplaner.repository.TaskRepository;
import com.haushaltsplaner.repository.TaskTemplateRepository;
import com.haushaltsplaner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskTemplateService {

    private final TaskTemplateRepository taskTemplateRepository;
    private final TaskRepository taskRepository;
    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TaskTemplateDto> getTemplatesForHousehold(Long householdId) {
        return taskTemplateRepository.findByHouseholdId(householdId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskTemplateDto createTemplate(TaskTemplateDto dto, Long householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found"));

        TaskTemplate template = new TaskTemplate();
        template.setName(dto.getName());
        template.setFrequency(dto.getFrequency());
        template.setDefaultPoints(dto.getDefaultPoints() != null ? dto.getDefaultPoints() : 1);
        template.setCompletionPeriodDays(dto.getCompletionPeriodDays());
        template.setHousehold(household);

        // Assign users if provided
        if (dto.getAssignedUserIds() != null && !dto.getAssignedUserIds().isEmpty()) {
            Set<User> users = dto.getAssignedUserIds().stream()
                    .map(id -> userRepository.findById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            template.setAssignedUsers(users);
        }

        return toDto(taskTemplateRepository.save(template));
    }

    @Transactional
    public TaskTemplateDto updateTemplate(Long templateId, TaskTemplateDto dto) {
        TaskTemplate template = taskTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        template.setName(dto.getName());
        template.setFrequency(dto.getFrequency());
        template.setDefaultPoints(dto.getDefaultPoints());
        template.setCompletionPeriodDays(dto.getCompletionPeriodDays());

        // Update assigned users
        if (dto.getAssignedUserIds() != null) {
            Set<User> users = dto.getAssignedUserIds().stream()
                    .map(id -> userRepository.findById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            template.setAssignedUsers(users);
        }

        return toDto(taskTemplateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        TaskTemplate template = taskTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        // Delete associated tasks first (avoid foreign key violation)
        List<Task> associatedTasks = taskRepository.findByTaskTemplateId(templateId);
        taskRepository.deleteAll(associatedTasks);
        
        // Clear assigned users relationship
        template.getAssignedUsers().clear();
        
        taskTemplateRepository.delete(template);
    }

    private TaskTemplateDto toDto(TaskTemplate template) {
        return TaskTemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .frequency(template.getFrequency())
                .defaultPoints(template.getDefaultPoints())
                .completionPeriodDays(template.getCompletionPeriodDays())
                .assignedUserIds(template.getAssignedUsers().stream()
                        .map(User::getId)
                        .collect(Collectors.toSet()))
                .build();
    }
}
