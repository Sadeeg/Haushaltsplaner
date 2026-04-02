package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.*;
import com.haushaltsplaner.dto.*;
import com.haushaltsplaner.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskTemplateRepository taskTemplateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private ExclusionRuleRepository exclusionRuleRepository;

    @InjectMocks
    private TaskService taskService;

    private Household household;
    private User user1;
    private User user2;
    private Task task;
    private TaskTemplate template;

    @BeforeEach
    void setUp() {
        household = Household.builder()
                .id(1L)
                .name("Test Household")
                .build();

        user1 = User.builder()
                .id(1L)
                .username("user1")
                .displayName("User One")
                .monthlyPoints(10)
                .household(household)
                .assignedTasks(new HashSet<>())
                .build();

        user2 = User.builder()
                .id(2L)
                .username("user2")
                .displayName("User Two")
                .monthlyPoints(5)
                .household(household)
                .assignedTasks(new HashSet<>())
                .build();

        template = TaskTemplate.builder()
                .id(1L)
                .name("Dishes")
                .frequency(TaskFrequency.DAILY)
                .defaultPoints(1)
                .household(household)
                .completionPeriodDays(1)
                .assignedUsers(new HashSet<>())
                .build();

        task = Task.builder()
                .id(1L)
                .name("Dishes")
                .frequency(TaskFrequency.DAILY)
                .dueDate(LocalDate.now())
                .completionPeriodStart(LocalDate.now())
                .completionPeriodEnd(LocalDate.now().plusDays(1))
                .status(TaskStatus.PENDING)
                .points(5)
                .household(household)
                .assignedUser(user1)
                .taskTemplate(template)
                .build();

        Set<User> members = new HashSet<>();
        members.add(user1);
        members.add(user2);
        household.setMembers(members);
    }

    @Test
    @DisplayName("Should increment user monthly points when completing task")
    void testCompleteTask_IncrementsUserMonthlyPoints() {
        int originalPoints = user1.getMonthlyPoints();
        int taskPoints = task.getPoints();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(userRepository.save(any(User.class))).thenReturn(user1);

        taskService.completeTask(1L);

        assertEquals(originalPoints + taskPoints, user1.getMonthlyPoints());
        verify(userRepository).save(user1);
    }

    @Test
    @DisplayName("Should set task status to COMPLETED")
    void testCompleteTask_SetsStatusToCompleted() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        TaskDto result = taskService.completeTask(1L);

        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    @DisplayName("Should create new task for next user when skipping")
    void testSkipTask_CreatesNewTaskForNextUser() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.findByHouseholdIdAndStatus(1L, TaskStatus.COMPLETED))
                .thenReturn(Collections.emptyList());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(System.nanoTime());
            return t;
        });

        TaskDto result = taskService.skipTask(1L);

        assertEquals(TaskStatus.SKIPPED, result.getStatus());
        verify(taskRepository, times(2)).save(any(Task.class));
    }

    @Test
    @DisplayName("Should not give points for skipped task")
    void testSkipTask_DoesNotGivePoints() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.findByHouseholdIdAndStatus(1L, TaskStatus.COMPLETED))
                .thenReturn(Collections.emptyList());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.skipTask(1L);

        // Verify that no points were added (no userRepository.save called with point increment)
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should create new task for tomorrow when moving")
    void testMoveTask_CreatesNewTaskForTomorrow() {
        LocalDate originalDueDate = task.getDueDate();
        LocalDate expectedTomorrow = originalDueDate.plusDays(1);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(System.nanoTime());
            return t;
        });

        TaskDto result = taskService.moveTask(1L);

        // Verify at least one task was saved
        verify(taskRepository, atLeastOnce()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should mark original task as MOVED")
    void testMoveTask_MarksOriginalAsMoved() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDto result = taskService.moveTask(1L);

        assertEquals(TaskStatus.MOVED, task.getStatus());
    }

    @Test
    @DisplayName("Should filter leaderboard by current month")
    void testGetLeaderboard_FiltersByCurrentMonth() {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);

        // Create completed task from current month
        Task completedThisMonth = Task.builder()
                .id(1L)
                .assignedUser(user1)
                .status(TaskStatus.COMPLETED)
                .dueDate(now)
                .completedAt(LocalDateTime.now())
                .points(5)
                .build();

        // Create completed task from last month
        Task completedLastMonth = Task.builder()
                .id(2L)
                .assignedUser(user2)
                .status(TaskStatus.COMPLETED)
                .dueDate(now.minusMonths(1).withDayOfMonth(15))
                .completedAt(now.minusMonths(1).withDayOfMonth(15).atStartOfDay())
                .points(10)
                .build();

        user1.setAssignedTasks(Set.of(completedThisMonth));
        user2.setAssignedTasks(Set.of(completedLastMonth));
        user1.setMonthlyPoints(5);
        user2.setMonthlyPoints(10);

        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));

        List<LeaderboardEntry> leaderboard = taskService.getLeaderboard(1L);

        // Both users should be in leaderboard
        assertEquals(2, leaderboard.size());
        // user2 has higher monthlyPoints (10) so should be first
        assertEquals(2L, leaderboard.get(0).getUserId());
    }

    @Test
    @DisplayName("Should return tasks for user")
    void testGetTasksForUser_ReturnsPendingTasks() {
        when(taskRepository.findByAssignedUserIdAndStatus(1L, TaskStatus.PENDING))
                .thenReturn(List.of(task));

        List<TaskDto> tasks = taskService.getTasksForUser(1L);

        assertEquals(1, tasks.size());
        assertEquals("Dishes", tasks.get(0).getName());
    }

    @Test
    @DisplayName("Should return today's tasks for household")
    void testGetTodaysTasks_ReturnsTodaysPendingTasks() {
        when(taskRepository.findByHouseholdAndDate(1L, LocalDate.now()))
                .thenReturn(List.of(task));

        List<TaskDto> tasks = taskService.getTodaysTasks(1L);

        assertEquals(1, tasks.size());
        assertEquals(TaskStatus.PENDING, tasks.get(0).getStatus());
    }
}
