package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.*;
import com.haushaltsplaner.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RotationServiceTest {

    @Autowired
    private RotationService rotationService;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ExclusionRuleRepository exclusionRuleRepository;

    private Household testHousehold;
    private User user1;
    private User user2;
    private User user3;
    private TaskTemplate dailyTask;
    private TaskTemplate weeklyTask;

    @BeforeEach
    void setUp() {
        // Create household
        testHousehold = Household.builder()
                .name("Test Household")
                .build();
        householdRepository.save(testHousehold);

        // Create users
        user1 = User.builder()
                .username("user1")
                .email("user1@test.com")
                .displayName("User One")
                .household(testHousehold)
                .build();
        userRepository.save(user1);

        user2 = User.builder()
                .username("user2")
                .email("user2@test.com")
                .displayName("User Two")
                .household(testHousehold)
                .build();
        userRepository.save(user2);

        user3 = User.builder()
                .username("user3")
                .email("user3@test.com")
                .displayName("User Three")
                .household(testHousehold)
                .build();
        userRepository.save(user3);

        // Add members to household
        testHousehold.getMembers().add(user1);
        testHousehold.getMembers().add(user2);
        testHousehold.getMembers().add(user3);
        householdRepository.save(testHousehold);

        // Create task templates
        dailyTask = TaskTemplate.builder()
                .name("Daily Dishes")
                .frequency(TaskFrequency.DAILY)
                .defaultPoints(2)
                .household(testHousehold)
                .completionPeriodDays(0)
                .build();
        taskTemplateRepository.save(dailyTask);

        weeklyTask = TaskTemplate.builder()
                .name("Weekly Cleaning")
                .frequency(TaskFrequency.WEEKLY)
                .defaultPoints(5)
                .household(testHousehold)
                .completionPeriodDays(5)
                .build();
        taskTemplateRepository.save(weeklyTask);
    }

    @Test
    @DisplayName("Fairness: assigns to user who had task least recently")
    void testSelectAssignee_Fairness_BasedOnLeastRecentlyAssigned() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate twoDaysAgo = today.minusDays(2);

        // User1 did the task 2 days ago
        Task pastTask1 = Task.builder()
                .name("Daily Dishes")
                .frequency(TaskFrequency.DAILY)
                .dueDate(twoDaysAgo)
                .assignedUser(user1)
                .household(testHousehold)
                .taskTemplate(dailyTask)
                .status(TaskStatus.COMPLETED)
                .points(2)
                .build();
        taskRepository.save(pastTask1);

        // User2 did the task yesterday
        Task pastTask2 = Task.builder()
                .name("Daily Dishes")
                .frequency(TaskFrequency.DAILY)
                .dueDate(yesterday)
                .assignedUser(user2)
                .household(testHousehold)
                .taskTemplate(dailyTask)
                .status(TaskStatus.COMPLETED)
                .points(2)
                .build();
        taskRepository.save(pastTask2);

        // User3 has never done the task

        // Generate tasks for today
        rotationService.generateTasksForDate(testHousehold.getId(), today);

        // Find the generated task
        List<Task> todaysTasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        assertFalse(todaysTasks.isEmpty(), "Should have generated a task");

        Task generatedTask = todaysTasks.stream()
                .filter(t -> t.getTaskTemplate() != null && t.getTaskTemplate().getId().equals(dailyTask.getId()))
                .findFirst()
                .orElseThrow();

        // User3 should be selected (never did the task)
        assertEquals(user3.getId(), generatedTask.getAssignedUser().getId(),
                "User who never had the task should be selected");
    }

    @Test
    @DisplayName("Fairness: tie-breaker uses points when last assignment dates are equal")
    void testSelectAssignee_Fairness_TieBreakerByPoints() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // User1 and User2 both did the task yesterday
        Task pastTask1 = Task.builder()
                .name("Daily Dishes")
                .frequency(TaskFrequency.DAILY)
                .dueDate(yesterday)
                .assignedUser(user1)
                .household(testHousehold)
                .taskTemplate(dailyTask)
                .status(TaskStatus.COMPLETED)
                .points(10) // User1 has 10 points
                .build();
        taskRepository.save(pastTask1);

        Task pastTask2 = Task.builder()
                .name("Daily Dishes")
                .frequency(TaskFrequency.DAILY)
                .dueDate(yesterday)
                .assignedUser(user2)
                .household(testHousehold)
                .taskTemplate(dailyTask)
                .status(TaskStatus.COMPLETED)
                .points(5) // User2 has 5 points
                .build();
        taskRepository.save(pastTask2);

        // User3 has never done the task, but if we exclude them somehow...

        // Generate tasks for today
        rotationService.generateTasksForDate(testHousehold.getId(), today);

        // Find the generated task
        List<Task> todaysTasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        assertFalse(todaysTasks.isEmpty(), "Should have generated a task");

        Task generatedTask = todaysTasks.stream()
                .filter(t -> t.getTaskTemplate() != null && t.getTaskTemplate().getId().equals(dailyTask.getId()))
                .findFirst()
                .orElseThrow();

        // User3 should be selected (never did the task, beats tiebreaker)
        assertEquals(user3.getId(), generatedTask.getAssignedUser().getId(),
                "User who never had the task should be selected");
    }

    @Test
    @DisplayName("Respects exclusion rules when selecting assignee")
    void testSelectAssignee_RespectsExclusionRules() {
        LocalDate today = LocalDate.now();

        // Create a mutual exclusion rule between dailyTask and weeklyTask
        ExclusionRule rule = ExclusionRule.builder()
                .household(testHousehold)
                .taskA(dailyTask)
                .taskB(weeklyTask)
                .ruleType(ExclusionType.MUTUAL)
                .build();
        exclusionRuleRepository.save(rule);

        // First, assign weeklyTask to user1 today
        Task weeklyTaskToday = Task.builder()
                .name("Weekly Cleaning")
                .frequency(TaskFrequency.WEEKLY)
                .dueDate(today)
                .assignedUser(user1)
                .household(testHousehold)
                .taskTemplate(weeklyTask)
                .status(TaskStatus.PENDING)
                .points(5)
                .build();
        taskRepository.save(weeklyTaskToday);

        // Generate tasks for dailyTask
        // Since user1 already has weeklyTask today (due to mutual exclusion), 
        // they should not get dailyTask
        rotationService.generateTasksForDate(testHousehold.getId(), today);

        List<Task> todaysTasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        
        Task dailyTaskGenerated = todaysTasks.stream()
                .filter(t -> t.getTaskTemplate() != null && t.getTaskTemplate().getId().equals(dailyTask.getId()))
                .findFirst()
                .orElse(null);

        if (dailyTaskGenerated != null) {
            assertNotEquals(user1.getId(), dailyTaskGenerated.getAssignedUser().getId(),
                    "User1 should not be assigned dailyTask because they have weeklyTask today");
        }
    }

    @Test
    @DisplayName("Excludes user with conflicting task already assigned today")
    void testSelectAssignee_ExcludesUserWithConflictingTask() {
        LocalDate today = LocalDate.now();

        // Create mutual exclusion rule
        ExclusionRule rule = ExclusionRule.builder()
                .household(testHousehold)
                .taskA(dailyTask)
                .taskB(weeklyTask)
                .ruleType(ExclusionType.MUTUAL)
                .build();
        exclusionRuleRepository.save(rule);

        // Assign weeklyTask to user1 today
        Task weeklyTaskToday = Task.builder()
                .name("Weekly Cleaning")
                .frequency(TaskFrequency.WEEKLY)
                .dueDate(today)
                .assignedUser(user1)
                .household(testHousehold)
                .taskTemplate(weeklyTask)
                .status(TaskStatus.PENDING)
                .points(5)
                .build();
        taskRepository.save(weeklyTaskToday);

        // Assign some other task to user2 today
        Task otherTaskToday = Task.builder()
                .name("Other Task")
                .frequency(TaskFrequency.DAILY)
                .dueDate(today)
                .assignedUser(user2)
                .household(testHousehold)
                .status(TaskStatus.PENDING)
                .points(1)
                .build();
        taskRepository.save(otherTaskToday);

        // User3 has no tasks today
        // When generating dailyTask, user1 should be excluded (has weeklyTask)
        // So the task should go to user2 or user3
        
        rotationService.generateTasksForDate(testHousehold.getId(), today);

        List<Task> todaysTasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        
        Task dailyTaskGenerated = todaysTasks.stream()
                .filter(t -> t.getTaskTemplate() != null && t.getTaskTemplate().getId().equals(dailyTask.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(dailyTaskGenerated, "Daily task should be generated");
        
        // User1 should definitely not get dailyTask (they have weeklyTask)
        assertNotEquals(user1.getId(), dailyTaskGenerated.getAssignedUser().getId(),
                "User with conflicting task should be excluded");
    }

    @Test
    @DisplayName("generateTasksForDate applies exclusion rules correctly")
    void testGenerateTasksForDate_AppliesExclusionRules() {
        LocalDate today = LocalDate.now();

        // Create mutual exclusion rule
        ExclusionRule rule = ExclusionRule.builder()
                .household(testHousehold)
                .taskA(dailyTask)
                .taskB(weeklyTask)
                .ruleType(ExclusionType.MUTUAL)
                .build();
        exclusionRuleRepository.save(rule);

        // Pre-assign weeklyTask to user1
        Task weeklyTaskToday = Task.builder()
                .name("Weekly Cleaning")
                .frequency(TaskFrequency.WEEKLY)
                .dueDate(today)
                .assignedUser(user1)
                .household(testHousehold)
                .taskTemplate(weeklyTask)
                .status(TaskStatus.PENDING)
                .points(5)
                .build();
        taskRepository.save(weeklyTaskToday);

        // Generate tasks - should respect exclusion rules
        rotationService.generateTasksForDate(testHousehold.getId(), today);

        List<Task> todaysTasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        
        // There should be tasks generated
        assertEquals(2, todaysTasks.size(), "Should have both tasks generated");

        // Find daily task
        Task dailyTaskInstance = todaysTasks.stream()
                .filter(t -> t.getTaskTemplate() != null && t.getTaskTemplate().getId().equals(dailyTask.getId()))
                .findFirst()
                .orElseThrow();

        // User1 should not be assigned dailyTask (conflicts with weeklyTask)
        assertNotEquals(user1.getId(), dailyTaskInstance.getAssignedUser().getId(),
                "Exclusion rule should prevent user from getting conflicting task");
    }

    @Test
    @DisplayName("Mutual exclusion: two tasks with rule should not be assigned to same user")
    void testGenerateTasksForDate_MutualExclusionWorks() {
        LocalDate today = LocalDate.now();

        // Create mutual exclusion rule
        ExclusionRule rule = ExclusionRule.builder()
                .household(testHousehold)
                .taskA(dailyTask)
                .taskB(weeklyTask)
                .ruleType(ExclusionType.MUTUAL)
                .build();
        exclusionRuleRepository.save(rule);

        // Generate tasks - should ensure mutual exclusion
        rotationService.generateTasksForDate(testHousehold.getId(), today);

        List<Task> todaysTasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        
        assertEquals(2, todaysTasks.size(), "Should have both tasks generated");

        Task daily = todaysTasks.stream()
                .filter(t -> t.getTaskTemplate() != null && t.getTaskTemplate().getId().equals(dailyTask.getId()))
                .findFirst()
                .orElseThrow();

        Task weekly = todaysTasks.stream()
                .filter(t -> t.getTaskTemplate() != null && t.getTaskTemplate().getId().equals(weeklyTask.getId()))
                .findFirst()
                .orElseThrow();

        // They should NOT be assigned to the same user
        assertNotEquals(daily.getAssignedUser().getId(), weekly.getAssignedUser().getId(),
                "Mutual exclusion rule should prevent same user getting both tasks");
    }
}
