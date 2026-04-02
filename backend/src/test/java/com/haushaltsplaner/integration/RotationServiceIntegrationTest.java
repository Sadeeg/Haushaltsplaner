package com.haushaltsplaner.integration;

import com.haushaltsplaner.domain.*;
import com.haushaltsplaner.repository.*;
import com.haushaltsplaner.service.RotationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RotationServiceIntegrationTest {

    @Autowired
    private RotationService rotationService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private ExclusionRuleRepository exclusionRuleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    private Household testHousehold;
    private User user1;
    private User user2;
    private User user3;
    private TaskTemplate dailyTemplate;

    @BeforeEach
    void setUp() {
        // Create test household
        testHousehold = Household.builder()
                .name("Rotation Test Household")
                .members(new HashSet<>())
                .taskTemplates(new HashSet<>())
                .exclusionRules(new HashSet<>())
                .build();
        testHousehold = householdRepository.save(testHousehold);

        // Create test users
        user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .displayName("User 1")
                .nextcloudId("nc-user1-123")
                .monthlyPoints(0)
                .household(testHousehold)
                .assignedTasks(new HashSet<>())
                .build();
        user1 = userRepository.save(user1);

        user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .displayName("User 2")
                .nextcloudId("nc-user2-123")
                .monthlyPoints(0)
                .household(testHousehold)
                .assignedTasks(new HashSet<>())
                .build();
        user2 = userRepository.save(user2);

        user3 = User.builder()
                .username("user3")
                .email("user3@example.com")
                .displayName("User 3")
                .nextcloudId("nc-user3-123")
                .monthlyPoints(0)
                .household(testHousehold)
                .assignedTasks(new HashSet<>())
                .build();
        user3 = userRepository.save(user3);

        testHousehold.getMembers().add(user1);
        testHousehold.getMembers().add(user2);
        testHousehold.getMembers().add(user3);
        testHousehold = householdRepository.save(testHousehold);

        // Create a daily task template
        dailyTemplate = TaskTemplate.builder()
                .name("Daily Task")
                .frequency(TaskFrequency.DAILY)
                .defaultPoints(5)
                .household(testHousehold)
                .assignedUsers(new HashSet<>())
                .build();
        dailyTemplate = taskTemplateRepository.save(dailyTemplate);
    }

    @Test
    @DisplayName("Should assign tasks to eligible users")
    void testGenerateTasksForDate_AssignsToEligibleUsers() {
        LocalDate today = LocalDate.now();
        
        rotationService.generateTasksForDate(testHousehold.getId(), today);
        
        List<Task> tasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        
        assertFalse(tasks.isEmpty());
        
        Task task = tasks.stream()
                .filter(t -> t.getName().equals("Daily Task"))
                .findFirst()
                .orElseThrow();
        
        // Task should be assigned to one of the users
        assertNotNull(task.getAssignedUser());
        assertTrue(task.getAssignedUser().getId() != null);
    }

    @Test
    @DisplayName("Should respect assigned users from template")
    void testGenerateTasksForDate_RespectsAssignedUsers() {
        // Create template with only user1 and user2 assigned
        TaskTemplate limitedTemplate = TaskTemplate.builder()
                .name("Limited Task")
                .frequency(TaskFrequency.DAILY)
                .defaultPoints(3)
                .household(testHousehold)
                .assignedUsers(new HashSet<>())
                .build();
        limitedTemplate.getAssignedUsers().add(user1);
        limitedTemplate.getAssignedUsers().add(user2);
        limitedTemplate = taskTemplateRepository.save(limitedTemplate);

        LocalDate today = LocalDate.now();
        
        rotationService.generateTasksForDate(testHousehold.getId(), today);
        
        List<Task> tasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        
        Task limitedTask = tasks.stream()
                .filter(t -> t.getName().equals("Limited Task"))
                .findFirst()
                .orElseThrow();
        
        // Should only be assigned to user1 or user2, not user3
        assertTrue(limitedTask.getAssignedUser().getId().equals(user1.getId()) ||
                   limitedTask.getAssignedUser().getId().equals(user2.getId()));
        assertNotEquals(user3.getId(), limitedTask.getAssignedUser().getId());
    }

    @Test
    @DisplayName("Should not duplicate tasks for same date")
    void testGenerateTasksForDate_DoesNotDuplicate() {
        LocalDate today = LocalDate.now();
        
        // Generate tasks twice for same date
        rotationService.generateTasksForDate(testHousehold.getId(), today);
        rotationService.generateTasksForDate(testHousehold.getId(), today);
        
        List<Task> tasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        
        // Should only have one task per template
        long dailyTaskCount = tasks.stream()
                .filter(t -> t.getName().equals("Daily Task"))
                .count();
        
        assertEquals(1, dailyTaskCount);
    }

    @Test
    @DisplayName("Should generate tasks consistently over multiple days")
    void testGenerateTasksForDate_GeneratesConsistentlyOverMultipleDays() {
        // Clear any existing tasks
        taskRepository.deleteAll();
        
        LocalDate today = LocalDate.now();
        
        // Generate tasks for multiple days
        for (int i = 0; i < 10; i++) {
            rotationService.generateTasksForDate(testHousehold.getId(), today.plusDays(i));
        }
        
        List<Task> allTasks = taskRepository.findAll().stream()
                .filter(t -> t.getHousehold().getId().equals(testHousehold.getId()))
                .toList();
        List<Task> dailyTasks = allTasks.stream()
                .filter(t -> t.getName().equals("Daily Task"))
                .toList();
        
        // Verify 10 daily tasks were created (one for each day)
        assertEquals(10, dailyTasks.size());
        
        // Verify all tasks are assigned to a user
        for (Task task : dailyTasks) {
            assertNotNull(task.getAssignedUser(), "Task should be assigned to a user");
        }
        
        // Verify each day has exactly one task
        for (int i = 0; i < 10; i++) {
            LocalDate date = today.plusDays(i);
            List<Task> tasksForDay = dailyTasks.stream()
                    .filter(t -> t.getDueDate().equals(date))
                    .toList();
            assertEquals(1, tasksForDay.size(), "Should have exactly one task for " + date);
        }
    }

    @Test
    @DisplayName("Should handle mutual exclusion rules")
    void testGenerateTasksForDate_RespectsExclusionRules() {
        // Create two tasks that should be mutually exclusive
        TaskTemplate taskA = TaskTemplate.builder()
                .name("Task A")
                .frequency(TaskFrequency.DAILY)
                .defaultPoints(3)
                .household(testHousehold)
                .assignedUsers(Set.of(user1, user2))
                .build();
        taskA = taskTemplateRepository.save(taskA);

        TaskTemplate taskB = TaskTemplate.builder()
                .name("Task B")
                .frequency(TaskFrequency.DAILY)
                .defaultPoints(3)
                .household(testHousehold)
                .assignedUsers(Set.of(user1, user2))
                .build();
        taskB = taskTemplateRepository.save(taskB);

        // Create mutual exclusion rule
        ExclusionRule rule = ExclusionRule.builder()
                .household(testHousehold)
                .ruleType(ExclusionType.MUTUAL)
                .taskA(taskA)
                .taskB(taskB)
                .build();
        exclusionRuleRepository.save(rule);

        LocalDate today = LocalDate.now();
        rotationService.generateTasksForDate(testHousehold.getId(), today);

        List<Task> tasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);

        Task taskADaily = tasks.stream()
                .filter(t -> t.getName().equals("Task A"))
                .findFirst()
                .orElseThrow();
        Task taskBDaily = tasks.stream()
                .filter(t -> t.getName().equals("Task B"))
                .findFirst()
                .orElseThrow();

        // Users assigned to Task A and Task B should be different
        assertNotEquals(taskADaily.getAssignedUser().getId(), taskBDaily.getAssignedUser().getId());
    }
}
