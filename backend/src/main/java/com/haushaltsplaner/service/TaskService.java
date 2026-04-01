package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.*;
import com.haushaltsplaner.dto.*;
import com.haushaltsplaner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final ExclusionRuleRepository exclusionRuleRepository;

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksForUser(Long userId) {
        return taskRepository.findByAssignedUserIdAndStatus(userId, TaskStatus.PENDING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksForUserOnDate(Long userId, LocalDate date) {
        return taskRepository.findByUserAndDateAndStatus(userId, date, TaskStatus.PENDING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTodaysTasks(Long householdId) {
        return taskRepository.findByHouseholdAndDate(householdId, LocalDate.now())
                .stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getPendingTasks(Long householdId) {
        return taskRepository.findByHouseholdIdAndStatus(householdId, TaskStatus.PENDING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksForWeek(Long householdId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1); // Monday
        LocalDate endOfWeek = startOfWeek.plusDays(6); // Sunday
        
        return taskRepository.findByDueDateBetweenAndStatus(startOfWeek, endOfWeek, TaskStatus.PENDING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDto createTask(CreateTaskRequest request, Long householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found"));

        Task task = Task.builder()
                .name(request.getName())
                .frequency(request.getFrequency())
                .dueDate(request.getDueDate() != null ? request.getDueDate() : LocalDate.now())
                .completionPeriodStart(request.getCompletionPeriodStart())
                .completionPeriodEnd(request.getCompletionPeriodEnd())
                .status(TaskStatus.PENDING)
                .points(request.getPoints() != null ? request.getPoints() : 1)
                .household(household)
                .build();

        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            task.setAssignedUser(user);
        }

        return toDto(taskRepository.save(task));
    }

    @Transactional
    public TaskDto completeTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        
        return toDto(taskRepository.save(task));
    }

    @Transactional
    public TaskDto skipTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Skip: task goes to next eligible user for TODAY
        // Original assignee gets no penalty, next person does it today
        User nextUser = findNextEligibleUserForSkip(task);
        
        if (nextUser != null) {
            // Create a new task for today assigned to next user
            Task newTask = Task.builder()
                    .name(task.getName())
                    .frequency(task.getFrequency())
                    .dueDate(LocalDate.now())
                    .completionPeriodStart(task.getCompletionPeriodStart())
                    .completionPeriodEnd(task.getCompletionPeriodEnd())
                    .status(TaskStatus.PENDING)
                    .points(0) // No points for skipped task
                    .household(task.getHousehold())
                    .assignedUser(nextUser)
                    .taskTemplate(task.getTaskTemplate())
                    .build();
            taskRepository.save(newTask);
        }
        
        // Mark original task as skipped
        task.setStatus(TaskStatus.SKIPPED);
        
        return toDto(taskRepository.save(task));
    }

    @Transactional
    public TaskDto moveTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Move: task moves to next day, original assignee still responsible tomorrow
        task.setStatus(TaskStatus.MOVED);
        LocalDate originalDueDate = task.getDueDate() != null ? task.getDueDate() : LocalDate.now();
        LocalDate tomorrow = originalDueDate.plusDays(1);
        
        // Create a new task for tomorrow with same assignee
        Task movedTask = Task.builder()
                .name(task.getName())
                .frequency(task.getFrequency())
                .dueDate(tomorrow)
                .completionPeriodStart(tomorrow)
                .completionPeriodEnd(tomorrow.plusDays(task.getTaskTemplate() != null && task.getTaskTemplate().getCompletionPeriodDays() != null ? 
                        task.getTaskTemplate().getCompletionPeriodDays() : 1))
                .status(TaskStatus.PENDING)
                .points(task.getPoints())
                .household(task.getHousehold())
                .assignedUser(task.getAssignedUser())
                .taskTemplate(task.getTaskTemplate())
                .build();
        
        taskRepository.save(movedTask);
        
        // Mark original task as moved
        task.setStatus(TaskStatus.MOVED);
        
        return toDto(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getLeaderboard(Long householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found"));

        return household.getMembers().stream()
                .map(user -> {
                    int completed = (int) user.getAssignedTasks().stream()
                            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                            .count();
                    int skipped = (int) user.getAssignedTasks().stream()
                            .filter(t -> t.getStatus() == TaskStatus.SKIPPED)
                            .count();
                    int totalPoints = user.getAssignedTasks().stream()
                            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                            .mapToInt(Task::getPoints)
                            .sum();

                    return LeaderboardEntry.builder()
                            .userId(user.getId())
                            .displayName(user.getDisplayName())
                            .totalPoints(totalPoints)
                            .completedTasks(completed)
                            .skippedTasks(skipped)
                            .build();
                })
                .sorted((a, b) -> b.getTotalPoints().compareTo(a.getTotalPoints()))
                .collect(Collectors.toList());
    }

    private User findNextEligibleUserForSkip(Task task) {
        Household household = task.getHousehold();
        List<User> members = List.copyOf(household.getMembers());
        
        if (members.isEmpty()) return null;

        // For skip: find the next user in rotation, skipping the current assignee
        // Use fair rotation based on who did the task least recently
        User current = task.getAssignedUser();
        
        // Get completed tasks for this template to determine fair rotation
        List<Task> completedTasks = taskRepository.findByHouseholdIdAndStatus(household.getId(), TaskStatus.COMPLETED)
                .stream()
                .filter(t -> t.getTaskTemplate() != null && 
                        t.getTaskTemplate().getId().equals(task.getTaskTemplate() != null ? task.getTaskTemplate().getId() : null))
                .sorted((a, b) -> {
                    LocalDate dateA = a.getDueDate() != null ? a.getDueDate() : LocalDate.MIN;
                    LocalDate dateB = b.getDueDate() != null ? b.getDueDate() : LocalDate.MIN;
                    return dateB.compareTo(dateA); // Most recent first
                })
                .collect(Collectors.toList());

        if (completedTasks.isEmpty()) {
            // No history, just pick next in simple rotation
            int currentIndex = current != null ? members.indexOf(current) : -1;
            int nextIndex = (currentIndex + 1) % members.size();
            return members.get(nextIndex);
        }

        // Find who did it last and pick the next person
        Task lastTask = completedTasks.get(0);
        User lastAssignee = lastTask.getAssignedUser();
        int lastIndex = lastAssignee != null ? members.indexOf(lastAssignee) : -1;
        int nextIndex = (lastIndex + 1) % members.size();
        
        // Skip the current assignee (they are skipping this task)
        if (members.get(nextIndex).getId().equals(current != null ? current.getId() : -1)) {
            nextIndex = (nextIndex + 1) % members.size();
        }
        
        return members.get(nextIndex);
    }

    private TaskDto toDto(Task task) {
        return TaskDto.builder()
                .id(task.getId())
                .name(task.getName())
                .frequency(task.getFrequency())
                .dueDate(task.getDueDate())
                .completionPeriodStart(task.getCompletionPeriodStart())
                .completionPeriodEnd(task.getCompletionPeriodEnd())
                .status(task.getStatus())
                .assignedUserId(task.getAssignedUser() != null ? task.getAssignedUser().getId() : null)
                .assignedUserName(task.getAssignedUser() != null ? task.getAssignedUser().getDisplayName() : null)
                .points(task.getPoints())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
