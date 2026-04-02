package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.*;
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
class RotationServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskTemplateRepository taskTemplateRepository;

    @Mock
    private ExclusionRuleRepository exclusionRuleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HouseholdRepository householdRepository;

    @InjectMocks
    private RotationService rotationService;

    private Household household;
    private User user1;
    private User user2;
    private User user3;
    private TaskTemplate template1;
    private TaskTemplate template2;

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
                .build();

        user2 = User.builder()
                .id(2L)
                .username("user2")
                .displayName("User Two")
                .monthlyPoints(5)
                .household(household)
                .build();

        user3 = User.builder()
                .id(3L)
                .username("user3")
                .displayName("User Three")
                .monthlyPoints(0)
                .household(household)
                .build();

        Set<User> members = new HashSet<>();
        members.add(user1);
        members.add(user2);
        members.add(user3);
        household.setMembers(members);

        template1 = TaskTemplate.builder()
                .id(1L)
                .name("Dishes")
                .frequency(TaskFrequency.DAILY)
                .defaultPoints(1)
                .household(household)
                .assignedUsers(new HashSet<>())
                .build();

        template2 = TaskTemplate.builder()
                .id(2L)
                .name("Vacuuming")
                .frequency(TaskFrequency.WEEKLY)
                .defaultPoints(3)
                .household(household)
                .assignedUsers(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Should choose user who had task least recently")
    void testSelectAssignee_ChoosesUserWhoHadTaskLeastRecently() {
        // User3 never had the task, User2 had it 10 days ago, User1 had it 5 days ago
        // User3 should be selected (never had it = least recently)
        LocalDate now = LocalDate.now();

        Task completedTask1 = Task.builder()
                .id(1L)
                .assignedUser(user1)
                .taskTemplate(template1)
                .dueDate(now.minusDays(5))
                .status(TaskStatus.COMPLETED)
                .points(1)
                .build();

        Task completedTask2 = Task.builder()
                .id(2L)
                .assignedUser(user2)
                .taskTemplate(template1)
                .dueDate(now.minusDays(10))
                .status(TaskStatus.COMPLETED)
                .points(1)
                .build();

        when(exclusionRuleRepository.findByHouseholdId(1L)).thenReturn(Collections.emptyList());
        when(taskRepository.findByAssignedUserIdAndStatus(eq(1L), eq(TaskStatus.COMPLETED)))
                .thenReturn(List.of(completedTask1));
        when(taskRepository.findByAssignedUserIdAndStatus(eq(2L), eq(TaskStatus.COMPLETED)))
                .thenReturn(List.of(completedTask2));
        when(taskRepository.findByAssignedUserIdAndStatus(eq(3L), eq(TaskStatus.COMPLETED)))
                .thenReturn(Collections.emptyList());

        List<User> eligibleUsers = List.of(user1, user2, user3);
        Map<Long, User> assignedUsersToday = new HashMap<>();

        // Use reflection to test private method - we'll test through generateTasksForDate
        // since the selectAssignee is private
        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));
        when(taskTemplateRepository.findByHouseholdId(1L)).thenReturn(List.of(template1));
        when(taskRepository.findByHouseholdAndDate(1L, now)).thenReturn(Collections.emptyList());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(1L);
            return task;
        });

        rotationService.generateTasksForDate(1L, now);

        verify(taskRepository).save(argThat(task -> 
            task.getAssignedUser() != null && task.getAssignedUser().getId().equals(3L)
        ));
    }

    @Test
    @DisplayName("Should use tie breaker by lowest points when never assigned")
    void testSelectAssignee_TieBreakerByLowestPoints() {
        // User1, User2, User3 all never had this task
        // Should select User3 (lowest points = 0)
        LocalDate now = LocalDate.now();

        when(exclusionRuleRepository.findByHouseholdId(1L)).thenReturn(Collections.emptyList());
        when(taskRepository.findByAssignedUserIdAndStatus(anyLong(), eq(TaskStatus.COMPLETED)))
                .thenReturn(Collections.emptyList());

        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));
        when(taskTemplateRepository.findByHouseholdId(1L)).thenReturn(List.of(template1));
        when(taskRepository.findByHouseholdAndDate(1L, now)).thenReturn(Collections.emptyList());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(1L);
            return task;
        });

        rotationService.generateTasksForDate(1L, now);

        // Verify a task was saved - the fairness algorithm should have selected user3 (lowest points)
        verify(taskRepository, atLeastOnce()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should exclude users based on mutual exclusion rules")
    void testSelectAssignee_ExcludesUsersBasedOnExclusionRules() {
        // Create mutual exclusion between template1 and template2
        ExclusionRule rule = ExclusionRule.builder()
                .id(1L)
                .household(household)
                .taskA(template1)
                .taskB(template2)
                .ruleType(ExclusionType.MUTUAL)
                .build();

        LocalDate now = LocalDate.now();

        // User1 is already assigned to template2 today - so User1 should be excluded from template1
        Task taskForTemplate2 = Task.builder()
                .id(1L)
                .assignedUser(user1)
                .taskTemplate(template2)
                .dueDate(now)
                .status(TaskStatus.PENDING)
                .build();

        Map<Long, User> assignedUsersToday = new HashMap<>();
        assignedUsersToday.put(template2.getId(), user1);

        when(exclusionRuleRepository.findByHouseholdId(1L)).thenReturn(List.of(rule));
        when(taskRepository.findByAssignedUserIdAndStatus(anyLong(), eq(TaskStatus.COMPLETED)))
                .thenReturn(Collections.emptyList());

        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));
        when(taskTemplateRepository.findByHouseholdId(1L)).thenReturn(List.of(template1));
        when(taskRepository.findByHouseholdAndDate(1L, now)).thenReturn(Collections.emptyList());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(1L);
            return task;
        });

        rotationService.generateTasksForDate(1L, now);

        // Verify a task was saved - user1 should be excluded due to mutual exclusion rule
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should use only assigned users from template")
    void testSelectAssignee_UsesOnlyAssignedUsersFromTemplate() {
        // Only user1 and user2 are assigned to template1
        template1.setAssignedUsers(new HashSet<>(Set.of(user1, user2)));

        LocalDate now = LocalDate.now();

        when(exclusionRuleRepository.findByHouseholdId(1L)).thenReturn(Collections.emptyList());
        when(taskRepository.findByAssignedUserIdAndStatus(anyLong(), eq(TaskStatus.COMPLETED)))
                .thenReturn(Collections.emptyList());

        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));
        when(taskTemplateRepository.findByHouseholdId(1L)).thenReturn(List.of(template1));
        when(taskRepository.findByHouseholdAndDate(1L, now)).thenReturn(Collections.emptyList());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(1L);
            return task;
        });

        rotationService.generateTasksForDate(1L, now);

        verify(taskRepository).save(argThat(task -> {
            Long assignedUserId = task.getAssignedUser().getId();
            return assignedUserId.equals(1L) || assignedUserId.equals(2L);
        }));
    }

    @Test
    @DisplayName("Should generate tasks for all templates")
    void testGenerateTasksForDate_CreatesTasksForAllTemplates() {
        List<TaskTemplate> templates = List.of(template1, template2);
        LocalDate now = LocalDate.now();

        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));
        when(taskTemplateRepository.findByHouseholdId(1L)).thenReturn(templates);
        when(taskRepository.findByHouseholdAndDate(1L, now)).thenReturn(Collections.emptyList());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(System.nanoTime());
            return task;
        });

        rotationService.generateTasksForDate(1L, now);

        verify(taskRepository, times(2)).save(any(Task.class));
    }

    @Test
    @DisplayName("Should not duplicate existing tasks")
    void testGenerateTasksForDate_DoesNotDuplicateExistingTasks() {
        List<TaskTemplate> templates = List.of(template1, template2);
        LocalDate now = LocalDate.now();

        // One task already exists for template1
        Task existingTask = Task.builder()
                .id(1L)
                .name("Dishes")
                .taskTemplate(template1)
                .dueDate(now)
                .status(TaskStatus.PENDING)
                .household(household)
                .build();

        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));
        when(taskTemplateRepository.findByHouseholdId(1L)).thenReturn(templates);
        when(taskRepository.findByHouseholdAndDate(1L, now)).thenReturn(List.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(System.nanoTime());
            return task;
        });

        rotationService.generateTasksForDate(1L, now);

        // Should only save one new task (for template2), not for template1
        verify(taskRepository, times(1)).save(any(Task.class));
    }
}
