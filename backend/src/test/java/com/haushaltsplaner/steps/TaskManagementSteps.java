package com.haushaltsplaner.steps;

import com.haushaltsplaner.domain.*;
import com.haushaltsplaner.dto.TaskDto;
import com.haushaltsplaner.repository.*;
import com.haushaltsplaner.service.TaskService;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TaskManagementSteps {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    private Household household;
    private User sascha;
    private User alexandra;

    @Given("a user {string} exists in household")
    public void aUserExistsInHousehold(String name) {
        household = householdRepository.save(Household.builder().name("Test Household").build());
        sascha = userRepository.save(User.builder()
                .username(name.toLowerCase())
                .email(name.toLowerCase() + "@test.com")
                .displayName(name)
                .household(household)
                .build());
    }

    @Given("^users {string} and {string} exist in household$")
    public void usersExistInHousehold(String name1, String name2) {
        household = householdRepository.save(Household.builder().name("Test Household").build());
        sascha = userRepository.save(User.builder()
                .username(name1.toLowerCase())
                .email(name1.toLowerCase() + "@test.com")
                .displayName(name1)
                .household(household)
                .build());
        alexandra = userRepository.save(User.builder()
                .username(name2.toLowerCase())
                .email(name2.toLowerCase() + "@test.com")
                .displayName(name2)
                .household(household)
                .build());
    }

    @Given("^{string} has a pending task {string} for today$")
    public void userHasPendingTaskForToday(String userName, String taskName) {
        User user = userName.equals("Sascha") ? sascha : alexandra;
        taskRepository.save(Task.builder()
                .name(taskName)
                .frequency(TaskFrequency.DAILY)
                .dueDate(LocalDate.now())
                .status(TaskStatus.PENDING)
                .household(household)
                .assignedUser(user)
                .build());
    }

    @When("^{string} completes the task {string}$")
    public void userCompletesTask(String userName, String taskName) {
        User user = userName.equals("Sascha") ? sascha : alexandra;
        List<TaskDto> tasks = taskService.getTasksForUserOnDate(user.getId(), LocalDate.now());
        TaskDto task = tasks.stream()
                .filter(t -> t.getName().equals(taskName))
                .findFirst()
                .orElseThrow();
        taskService.completeTask(task.getId());
    }

    @Then("the task {string} is marked as completed")
    public void taskIsMarkedAsCompleted(String taskName) {
        List<Task> tasks = taskRepository.findByHouseholdAndDate(household.getId(), LocalDate.now());
        Task task = tasks.stream()
                .filter(t -> t.getName().equals(taskName))
                .findFirst()
                .orElseThrow();
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertNotNull(task.getCompletedAt());
    }

    @And("the points for {string} increase by {int}")
    public void pointsIncrease(String userName, int expectedPoints) {
        User user = userName.equals("Sascha") ? sascha : alexandra;
        List<Task> completedTasks = taskRepository.findByAssignedUserIdAndStatus(user.getId(), TaskStatus.COMPLETED);
        int totalPoints = completedTasks.stream().mapToInt(Task::getPoints).sum();
        assertEquals(expectedPoints, totalPoints);
    }

    @When("^{string} skips the task {string}$")
    public void userSkipsTask(String userName, String taskName) {
        User user = userName.equals("Sascha") ? sascha : alexandra;
        List<TaskDto> tasks = taskService.getTasksForUserOnDate(user.getId(), LocalDate.now());
        TaskDto task = tasks.stream()
                .filter(t -> t.getName().equals(taskName))
                .findFirst()
                .orElseThrow();
        taskService.skipTask(task.getId());
    }

    @Then("the task {string} is marked as skipped")
    public void taskIsMarkedAsSkipped(String taskName) {
        List<Task> tasks = taskRepository.findByHouseholdAndDate(household.getId(), LocalDate.now());
        Task task = tasks.stream()
                .filter(t -> t.getName().equals(taskName))
                .findFirst()
                .orElseThrow();
        assertEquals(TaskStatus.SKIPPED, task.getStatus());
    }

    @And("^{string} has a new task {string} for tomorrow$")
    public void userHasNewTaskForTomorrow(String userName, String taskName) {
        User user = userName.equals("Sascha") ? sascha : alexandra;
        List<Task> tomorrowTasks = taskRepository.findByHouseholdAndDate(household.getId(), LocalDate.now().plusDays(1));
        boolean found = tomorrowTasks.stream()
                .anyMatch(t -> t.getName().equals(taskName) && t.getAssignedUser().getId().equals(user.getId()));
        assertTrue(found);
    }

    @And("the points for {string} remain unchanged")
    public void pointsRemainUnchanged(String userName) {
        User user = userName.equals("Sascha") ? sascha : alexandra;
        List<Task> completedTasks = taskRepository.findByAssignedUserIdAndStatus(user.getId(), TaskStatus.COMPLETED);
        assertEquals(0, completedTasks.size());
    }

    @When("^{string} moves the task {string} to tomorrow$")
    public void userMovesTaskToTomorrow(String userName, String taskName) {
        User user = userName.equals("Sascha") ? sascha : alexandra;
        List<TaskDto> tasks = taskService.getTasksForUserOnDate(user.getId(), LocalDate.now());
        TaskDto task = tasks.stream()
                .filter(t -> t.getName().equals(taskName))
                .findFirst()
                .orElseThrow();
        taskService.moveTask(task.getId());
    }

    @Then("the task {string} is marked as moved")
    public void taskIsMarkedAsMoved(String taskName) {
        List<Task> tomorrowTasks = taskRepository.findByHouseholdAndDate(household.getId(), LocalDate.now().plusDays(1));
        Task task = tomorrowTasks.stream()
                .filter(t -> t.getName().equals(taskName))
                .findFirst()
                .orElseThrow();
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals(LocalDate.now().plusDays(1), task.getDueDate());
    }

    @And("^{string} has the task {string} for tomorrow$")
    public void userHasTaskForTomorrow(String userName, String taskName) {
        User user = userName.equals("Sascha") ? sascha : alexandra;
        List<Task> tomorrowTasks = taskRepository.findByHouseholdAndDate(household.getId(), LocalDate.now().plusDays(1));
        boolean found = tomorrowTasks.stream()
                .anyMatch(t -> t.getName().equals(taskName) && t.getAssignedUser().getId().equals(user.getId()));
        assertTrue(found);
    }

    @Given("^{string} has tasks {string}, {string} for today$")
    public void userHasTasksForToday(String userName, String task1, String task2) {
        User user = userName.equals("Sascha") ? sascha : alexandra;
        taskRepository.save(Task.builder()
                .name(task1)
                .frequency(TaskFrequency.DAILY)
                .dueDate(LocalDate.now())
                .status(TaskStatus.PENDING)
                .household(household)
                .assignedUser(user)
                .build());
        taskRepository.save(Task.builder()
                .name(task2)
                .frequency(TaskFrequency.DAILY)
                .dueDate(LocalDate.now())
                .status(TaskStatus.PENDING)
                .household(household)
                .assignedUser(user)
                .build());
    }

    @When("I request today's tasks")
    public void requestTodaysTasks() {
        List<TaskDto> tasks = taskService.getTodaysTasks(household.getId());
        assertNotNull(tasks);
    }
}
