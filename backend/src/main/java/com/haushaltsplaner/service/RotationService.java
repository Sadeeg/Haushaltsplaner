package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.*;
import com.haushaltsplaner.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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

        // Get today's already assigned tasks and their users (for exclusion rules)
        List<Task> todaysTasks = taskRepository.findByHouseholdAndDate(householdId, date);
        Map<Long, User> assignedUsersToday = todaysTasks.stream()
                .filter(t -> t.getAssignedUser() != null)
                .collect(Collectors.toMap(
                        t -> t.getTaskTemplate() != null ? t.getTaskTemplate().getId() : t.getId(),
                        Task::getAssignedUser,
                        (a, b) -> a
                ));

        for (TaskTemplate template : templates) {
            // Check if task already exists for this date
            boolean taskExists = todaysTasks.stream()
                    .anyMatch(t -> t.getTaskTemplate() != null && t.getTaskTemplate().getId().equals(template.getId()));

            if (!taskExists) {
                User assignee = selectAssignee(members, template, householdId, assignedUsersToday);
                
                int periodDays = template.getCompletionPeriodDays() != null ? template.getCompletionPeriodDays() : getDefaultPeriodDays(template.getFrequency());
                
                Task task = Task.builder()
                        .name(template.getName())
                        .frequency(template.getFrequency())
                        .dueDate(date)
                        .completionPeriodStart(date)
                        .completionPeriodEnd(date.plusDays(periodDays))
                        .status(TaskStatus.PENDING)
                        .points(template.getDefaultPoints() != null ? template.getDefaultPoints() : 1)
                        .household(household)
                        .taskTemplate(template)
                        .assignedUser(assignee)
                        .build();

                taskRepository.save(task);
                
                // Track this assignment for exclusion rules
                assignedUsersToday.put(template.getId(), assignee);
            }
        }
    }

    /**
     * Fairness algorithm for selecting the best assignee:
     * 1. Filter out users who cannot be assigned due to exclusion rules
     * 2. Among eligible users, pick the one who had this task least recently
     * 3. If tie, pick the one with lowest total points
     */
    private User selectAssignee(List<User> members, TaskTemplate template, Long householdId, Map<Long, User> assignedUsersToday) {
        List<ExclusionRule> rules = exclusionRuleRepository.findByHouseholdId(householdId);
        
        // Filter users based on exclusion rules
        List<User> eligibleUsers = members.stream()
                .filter(user -> isEligibleForTask(user, template, rules, assignedUsersToday))
                .collect(Collectors.toList());

        if (eligibleUsers.isEmpty()) {
            log.warn("No eligible user for task {} in household {}. Using random member.", template.getName(), householdId);
            return members.get(0);
        }

        // Score each user based on fairness criteria
        Map<User, UserScore> userScores = new HashMap<>();
        for (User user : eligibleUsers) {
            LocalDate lastAssigned = getLastAssignedDate(user.getId(), template.getId());
            int totalPoints = getTotalPoints(user.getId());
            userScores.put(user, new UserScore(lastAssigned, totalPoints));
        }

        // Sort by: least recently assigned (null = never), then lowest points
        return eligibleUsers.stream()
                .min((a, b) -> {
                    UserScore scoreA = userScores.get(a);
                    UserScore scoreB = userScores.get(b);
                    
                    // Never assigned comes first
                    if (scoreA.lastAssigned == null && scoreB.lastAssigned == null) {
                        return Integer.compare(scoreA.totalPoints, scoreB.totalPoints);
                    }
                    if (scoreA.lastAssigned == null) return -1;
                    if (scoreB.lastAssigned == null) return 1;
                    
                    // Less recent assignment comes first
                    int dateCompare = scoreA.lastAssigned.compareTo(scoreB.lastAssigned);
                    if (dateCompare != 0) return dateCompare;
                    
                    // Tie breaker: lower points
                    return Integer.compare(scoreA.totalPoints, scoreB.totalPoints);
                })
                .orElse(eligibleUsers.get(0));
    }

    private boolean isEligibleForTask(User user, TaskTemplate template, List<ExclusionRule> rules, Map<Long, User> assignedUsersToday) {
        for (ExclusionRule rule : rules) {
            // Check if this rule involves the current template
            boolean ruleApplies = false;
            if (rule.getRuleType() == ExclusionType.MUTUAL) {
                // For mutual exclusion, check if the other task in the rule is assigned today
                if (rule.getTaskA() != null && rule.getTaskA().getId().equals(template.getId())) {
                    ruleApplies = true;
                    // If TaskB is assigned to this user today, user is not eligible
                    User taskBAssignee = assignedUsersToday.get(rule.getTaskB().getId());
                    if (taskBAssignee != null && taskBAssignee.getId().equals(user.getId())) {
                        return false;
                    }
                }
                if (rule.getTaskB() != null && rule.getTaskB().getId().equals(template.getId())) {
                    ruleApplies = true;
                    // If TaskA is assigned to this user today, user is not eligible
                    User taskAAssignee = assignedUsersToday.get(rule.getTaskA().getId());
                    if (taskAAssignee != null && taskAAssignee.getId().equals(user.getId())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private LocalDate getLastAssignedDate(Long userId, Long templateId) {
        List<Task> userTasks = taskRepository.findByAssignedUserIdAndStatus(userId, TaskStatus.COMPLETED);
        return userTasks.stream()
                .filter(t -> t.getTaskTemplate() != null && t.getTaskTemplate().getId().equals(templateId))
                .map(Task::getDueDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private int getTotalPoints(Long userId) {
        List<Task> userTasks = taskRepository.findByAssignedUserIdAndStatus(userId, TaskStatus.COMPLETED);
        return userTasks.stream()
                .mapToInt(t -> t.getPoints() != null ? t.getPoints() : 0)
                .sum();
    }

    private int getDefaultPeriodDays(TaskFrequency frequency) {
        if (frequency == null) return 1;
        return switch (frequency) {
            case DAILY -> 0; // Same day
            case WEEKLY -> 5; // Until Friday
            case BI_WEEKLY -> 13; // Two weeks
            case MONTHLY -> 28; // Approximately one month
            default -> 1;
        };
    }

    // Helper class for scoring users
    private static class UserScore {
        final LocalDate lastAssigned;
        final int totalPoints;

        UserScore(LocalDate lastAssigned, int totalPoints) {
            this.lastAssigned = lastAssigned;
            this.totalPoints = totalPoints;
        }
    }
}
