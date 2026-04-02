package com.haushaltsplaner.integration;

import com.haushaltsplaner.domain.*;
import com.haushaltsplaner.dto.TaskTemplateDto;
import com.haushaltsplaner.repository.*;
import com.haushaltsplaner.service.RotationService;
import com.haushaltsplaner.service.TaskTemplateService;
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
public class TaskTemplateControllerIntegrationTest {

    @Autowired
    private TaskTemplateService taskTemplateService;

    @Autowired
    private RotationService rotationService;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    private Household testHousehold;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test household
        testHousehold = Household.builder()
                .name("Test Household")
                .members(new HashSet<>())
                .taskTemplates(new HashSet<>())
                .build();
        testHousehold = householdRepository.save(testHousehold);

        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .nextcloudId("nc-test-123")
                .monthlyPoints(0)
                .household(testHousehold)
                .assignedTasks(new HashSet<>())
                .build();
        testUser = userRepository.save(testUser);
        testHousehold.getMembers().add(testUser);
        testHousehold = householdRepository.save(testHousehold);
    }

    @Test
    @DisplayName("Should create template and generate tasks for today")
    void testCreateTemplate_GeneratesTasksForToday() {
        TaskTemplateDto dto = TaskTemplateDto.builder()
                .name("Daily Cleaning")
                .frequency(TaskFrequency.DAILY)
                .defaultPoints(5)
                .completionPeriodDays(0)
                .build();

        TaskTemplateDto created = taskTemplateService.createTemplate(dto, testHousehold.getId());

        // Trigger task generation for today
        LocalDate today = LocalDate.now();
        rotationService.generateTasksForDate(testHousehold.getId(), today);

        // Verify template was created
        assertNotNull(created.getId());
        assertEquals("Daily Cleaning", created.getName());

        // Verify tasks were generated for today
        List<Task> todaysTasks = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        assertFalse(todaysTasks.isEmpty());
        
        boolean foundDailyCleaning = todaysTasks.stream()
                .anyMatch(t -> t.getName().equals("Daily Cleaning"));
        assertTrue(foundDailyCleaning);
    }

    @Test
    @DisplayName("Should create template and generate tasks for next 14 days")
    void testCreateTemplate_GeneratesTasksForNext14Days() {
        TaskTemplateDto dto = TaskTemplateDto.builder()
                .name("Weekly Shopping")
                .frequency(TaskFrequency.WEEKLY)
                .defaultPoints(10)
                .completionPeriodDays(5)
                .build();

        TaskTemplateDto created = taskTemplateService.createTemplate(dto, testHousehold.getId());

        // Trigger task generation for today and next 14 days
        LocalDate today = LocalDate.now();
        for (int i = 0; i <= 14; i++) {
            rotationService.generateTasksForDate(testHousehold.getId(), today.plusDays(i));
        }

        // Verify template was created
        assertNotNull(created.getId());

        // Count total tasks generated (should have tasks for today + 14 days = 15 tasks)
        List<Task> allTasks = taskRepository.findAll().stream()
                .filter(t -> t.getHousehold().getId().equals(testHousehold.getId()))
                .toList();
        long shoppingTasks = allTasks.stream()
                .filter(t -> t.getName().equals("Weekly Shopping"))
                .count();

        assertEquals(15, shoppingTasks);
    }

    @Test
    @DisplayName("Should not affect existing tasks when deleting template")
    void testDeleteTemplate_DoesNotAffectExistingTasks() {
        // Create and generate tasks
        TaskTemplateDto dto = TaskTemplateDto.builder()
                .name("Temp Task")
                .frequency(TaskFrequency.DAILY)
                .defaultPoints(3)
                .build();

        TaskTemplateDto created = taskTemplateService.createTemplate(dto, testHousehold.getId());

        LocalDate today = LocalDate.now();
        rotationService.generateTasksForDate(testHousehold.getId(), today);

        // Get tasks before deletion
        List<Task> tasksBefore = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        int taskCountBefore = tasksBefore.size();
        Task existingTask = tasksBefore.stream()
                .filter(t -> t.getName().equals("Temp Task"))
                .findFirst()
                .orElseThrow();

        // Delete the template
        taskTemplateService.deleteTemplate(created.getId());

        // Verify template is deleted
        assertTrue(taskTemplateRepository.findById(created.getId()).isEmpty());

        // Verify existing tasks still exist
        List<Task> tasksAfter = taskRepository.findByHouseholdAndDate(testHousehold.getId(), today);
        assertEquals(taskCountBefore, tasksAfter.size());

        // Verify the specific task still exists
        Task stillExists = taskRepository.findById(existingTask.getId()).orElseThrow();
        assertEquals("Temp Task", stillExists.getName());
        assertEquals(TaskStatus.PENDING, stillExists.getStatus());
    }

    @Test
    @DisplayName("Should create template with specific assigned users")
    void testCreateTemplate_WithAssignedUsers() {
        // Create another user
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .displayName("User 2")
                .nextcloudId("nc-user2-123")
                .monthlyPoints(0)
                .household(testHousehold)
                .assignedTasks(new HashSet<>())
                .build();
        user2 = userRepository.save(user2);

        Set<Long> assignedUserIds = Set.of(testUser.getId(), user2.getId());

        TaskTemplateDto dto = TaskTemplateDto.builder()
                .name("Assigned Task")
                .frequency(TaskFrequency.WEEKLY)
                .defaultPoints(5)
                .assignedUserIds(assignedUserIds)
                .build();

        TaskTemplateDto created = taskTemplateService.createTemplate(dto, testHousehold.getId());

        assertNotNull(created.getAssignedUserIds());
        assertEquals(2, created.getAssignedUserIds().size());
        assertTrue(created.getAssignedUserIds().contains(testUser.getId()));
        assertTrue(created.getAssignedUserIds().contains(user2.getId()));
    }

    @Test
    @DisplayName("Should get all templates for household")
    void testGetTemplatesForHousehold() {
        // Create multiple templates
        TaskTemplateDto dto1 = TaskTemplateDto.builder()
                .name("Task 1")
                .frequency(TaskFrequency.DAILY)
                .build();
        taskTemplateService.createTemplate(dto1, testHousehold.getId());

        TaskTemplateDto dto2 = TaskTemplateDto.builder()
                .name("Task 2")
                .frequency(TaskFrequency.WEEKLY)
                .build();
        taskTemplateService.createTemplate(dto2, testHousehold.getId());

        List<TaskTemplateDto> templates = taskTemplateService.getTemplatesForHousehold(testHousehold.getId());

        assertEquals(2, templates.size());
    }
}
