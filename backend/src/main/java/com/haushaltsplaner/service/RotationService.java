package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.*;
import com.haushaltsplaner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RotationService {

    private final TaskRepository taskRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final ExclusionRuleRepository exclusionRuleRepository;
    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;

    @Transactional
    public void generateTasksForDate(Long householdId, LocalDate date) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found"));

        List<TaskTemplate> templates = taskTemplateRepository.findByHouseholdId(householdId);
        List<User> members = List.copyOf(household.getMembers());

        if (members.isEmpty()) return;

        for (TaskTemplate template : templates) {
            // Check if task already exists for this date
            List<Task> existing = taskRepository.findByHouseholdAndDate(householdId, date);
            boolean taskExists = existing.stream()
                    .anyMatch(t -> t.getTaskTemplate() != null && t.getTaskTemplate().getId().equals(template.getId()));

            if (!taskExists) {
                User assignee = selectAssignee(members, template, householdId);
                
                Task task = Task.builder()
                        .name(template.getName())
                        .frequency(template.getFrequency())
                        .dueDate(date)
                        .completionPeriodStart(date)
                        .completionPeriodEnd(date.plusDays(template.getCompletionPeriodDays() != null ? template.getCompletionPeriodDays() : 1))
                        .status(TaskStatus.PENDING)
                        .points(template.getDefaultPoints())
                        .household(household)
                        .taskTemplate(template)
                        .assignedUser(assignee)
                        .build();

                taskRepository.save(task);
            }
        }
    }

    private User selectAssignee(List<User> members, TaskTemplate template, Long householdId) {
        List<ExclusionRule> rules = exclusionRuleRepository.findByHouseholdId(householdId);
        
        // Find users who did the template's task last time
        List<Task> lastTasks = taskRepository.findByHouseholdIdAndStatus(householdId, TaskStatus.COMPLETED);
        
        // Simple round-robin: pick the member who had this task least recently
        // For now, just rotate based on index
        int index = (int) (System.currentTimeMillis() % members.size());
        return members.get(Math.abs(index));
    }
}
