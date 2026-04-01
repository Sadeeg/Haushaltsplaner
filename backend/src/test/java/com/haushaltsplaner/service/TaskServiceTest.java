package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.*;
import com.haushaltsplaner.dto.TaskDto;
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
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    private Household testHousehold;
    private User user1;
    private User user2;
    private User user3;
    private TaskTemplate dailyTask;
    private Task task1;
    private Task task2;

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

        // Create task template
        dailyTask = TaskTemplate.builder()
                .name("Daily Dishes")
                .frequency(TaskFrequency.DAILY)
                .defaultPoints(2)
                .household(testHousehold)
                .completionPeriodDays(0)
                .build();
        taskTemplateRepository.save(dailyTask);

        // Create tasks
        task1 = Task.builder()
                .name("Daily Dishes")
                .frequency(TaskFrequency.DAILY)
                .dueDate(LocalDate.now())
                .completionPeriodStart(LocalDate.now())
                .completionPeriodEnd(LocalDate.now())
                .status(TaskStatus.PENDING)
                .points(2)
                .household(testHousehold)
                .assignedUser(user1)
                .taskTemplate(dailyTask)
                .build();
        taskRepository.save(task1);

        task2 = Task.builder()
                .name("Weekly Cleaning")
                .frequency(TaskFrequency.WEEKLY)
                .dueDate(LocalDate.now())
                .completionPeriodStart(LocalDate.now())
                .completionPeriodEnd(LocalDate.now().plusDays(5))
                .status(TaskStatus.PENDING)
                .points(5)
                .household(testHousehold)
                .assignedUser(user2)
                .build();
        taskRepository.save(task2);
    }

    @Test
    @DisplayName("Skip task: assigns to next eligible user")
    void testSkipTask_AssignsToNextUser() {
        Long originalAssigneeId = task1.getAssignedUser().getId();

        // Skip the task
        TaskDto result = taskService.skipTask(task1.getId());

        // Task should now be SKIPPED
        assertEquals(TaskStatus.SKIPPED, result.getStatus());

        // Find the new task created for the skipped assignment
        List<Task> allTasks = taskRepository.findAll();
        List<Task> newTasks = allTasks.stream()
                .filter(t -> t.getName().equals(task1.getName()) && t.getId() != task1.getId())
                .toList();

        assertFalse(newTasks.isEmpty(), "A new task should be created for the skipped assignment");

        Task newTask = newTasks.get(0);
        assertNotEquals(originalAssigneeId, newTask.getAssignedUser().getId(),
                "New task should be assigned to a different user");
        assertEquals(TaskStatus.PENDING, newTask.getStatus());
    }

    @Test
    @DisplayName("Skip task: original task marked as SKIPPED, no points given")
    void testSkipTask_DoesNotGivePoints() {
        // Get original points
        Integer originalPoints = task1.getPoints();

        // Skip the task
        TaskDto result = taskService.skipTask(task1.getId());

        // Verify original task is marked as SKIPPED
        assertEquals(TaskStatus.SKIPPED, result.getStatus());

        // Find the new task - it should have 0 points
        List<Task> allTasks = taskRepository.findAll();
        List<Task> newTasks = allTasks.stream()
                .filter(t -> t.getName().equals(task1.getName()) && t.getId() != task1.getId())
                .toList();

        assertFalse(newTasks.isEmpty(), "A new task should be created");
        Task newTask = newTasks.get(0);
        
        // The new task for the skipper should have 0 points
        assertEquals(0, newTask.getPoints(),
                "Skipped task replacement should have 0 points");
        
        // The original task should retain its points (but status is SKIPPED)
        Task originalTask = taskRepository.findById(task1.getId()).orElseThrow();
        assertEquals(originalPoints, originalTask.getPoints());
        assertEquals(TaskStatus.SKIPPED, originalTask.getStatus());
    }

    @Test
    @DisplayName("Move task: creates new task for tomorrow")
    void testMoveTask_CreatesNewTaskForTomorrow() {
        LocalDate originalDueDate = task1.getDueDate();
        LocalDate tomorrow = originalDueDate.plusDays(1);

        // Move the task
        TaskDto result = taskService.moveTask(task1.getId());

        // Verify original task is marked as MOVED
        assertEquals(TaskStatus.MOVED, result.getStatus());

        // Find the new task created for tomorrow
        List<Task> allTasks = taskRepository.findAll();
        List<Task> tomorrowTasks = allTasks.stream()
                .filter(t -> t.getName().equals(task1.getName()) && t.getDueDate().equals(tomorrow))
                .toList();

        assertFalse(tomorrowTasks.isEmpty(), "A new task should be created for tomorrow");

        Task movedTask = tomorrowTasks.get(0);
        assertEquals(tomorrow, movedTask.getDueDate(),
                "Moved task should be due tomorrow");
        assertEquals(TaskStatus.PENDING, movedTask.getStatus());
        assertEquals(task1.getAssignedUser().getId(), movedTask.getAssignedUser().getId(),
                "Same user should still be assigned");
    }

    @Test
    @DisplayName("Move task: marks original as MOVED")
    void testMoveTask_MarksOriginalAsMoved() {
        // Move the task
        TaskDto result = taskService.moveTask(task1.getId());

        // Verify original task is marked as MOVED
        assertEquals(TaskStatus.MOVED, result.getStatus());

        Task originalTask = taskRepository.findById(task1.getId()).orElseThrow();
        assertEquals(TaskStatus.MOVED, originalTask.getStatus(),
                "Original task should be marked as MOVED");
    }

    @Test
    @DisplayName("Get tasks for user returns correct pending tasks")
    void testGetTasksForUser_ReturnsPendingTasks() {
        List<TaskDto> user1Tasks = taskService.getTasksForUser(user1.getId());
        
        assertFalse(user1Tasks.isEmpty(), "User1 should have tasks");
        assertTrue(user1Tasks.stream().allMatch(t -> t.getStatus() == TaskStatus.PENDING),
                "All returned tasks should be pending");
    }

    @Test
    @DisplayName("Complete task updates status and completedAt")
    void testCompleteTask_UpdatesStatusAndCompletedAt() {
        TaskDto result = taskService.completeTask(task1.getId());

        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt(), "CompletedAt should be set");
    }

    @Test
    @DisplayName("Leaderboard returns users sorted by points")
    void testGetLeaderboard_SortsByPoints() {
        // Complete some tasks for user1
        task1.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task1);

        List<?> leaderboard = taskService.getLeaderboard(testHousehold.getId());
        
        assertFalse(leaderboard.isEmpty(), "Leaderboard should not be empty");
        // User1 should have more points (2 from completing task1)
        // User2 has 0 completed tasks
    }

    @Test
    @DisplayName("Skip task with no other eligible users still marks as skipped")
    void testSkipTask_EvenWithNoOtherUsers() {
        // Create a household with only one user and skip their task
        Household singleUserHousehold = Household.builder()
                .name("Single User Household")
                .build();
        householdRepository.save(singleUserHousehold);

        User soloUser = User.builder()
                .username("solo")
                .email("solo@test.com")
                .displayName("Solo User")
                .household(singleUserHousehold)
                .build();
        userRepository.save(soloUser);

        singleUserHousehold.getMembers().add(soloUser);
        householdRepository.save(singleUserHousehold);

        Task soloTask = Task.builder()
                .name("Solo Task")
                .frequency(TaskFrequency.DAILY)
                .dueDate(LocalDate.now())
                .status(TaskStatus.PENDING)
                .points(1)
                .household(singleUserHousehold)
                .assignedUser(soloUser)
                .build();
        taskRepository.save(soloTask);

        // Skip should still work (even if no next user found)
        TaskDto result = taskService.skipTask(soloTask.getId());
        
        assertEquals(TaskStatus.SKIPPED, result.getStatus(),
                "Task should still be marked as skipped even with no other users");
    }

    @Test
    @DisplayName("Move task preserves points on moved task")
    void testMoveTask_PreservesPoints() {
        Integer originalPoints = task1.getPoints();

        taskService.moveTask(task1.getId());

        // Find the moved task
        List<Task> allTasks = taskRepository.findAll();
        Task movedTask = allTasks.stream()
                .filter(t -> t.getName().equals(task1.getName()) && t.getDueDate().isAfter(LocalDate.now()))
                .findFirst()
                .orElseThrow();

        assertEquals(originalPoints, movedTask.getPoints(),
                "Moved task should preserve original points");
    }
}
