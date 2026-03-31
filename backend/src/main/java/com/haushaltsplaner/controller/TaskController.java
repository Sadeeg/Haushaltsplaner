package com.haushaltsplaner.controller;

import com.haushaltsplaner.dto.CreateTaskRequest;
import com.haushaltsplaner.dto.LeaderboardEntry;
import com.haushaltsplaner.dto.TaskDto;
import com.haushaltsplaner.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskDto>> getTasksForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(taskService.getTasksForUser(userId));
    }

    @GetMapping("/user/{userId}/date/{date}")
    public ResponseEntity<List<TaskDto>> getTasksForUserOnDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(taskService.getTasksForUserOnDate(userId, date));
    }

    @GetMapping("/household/{householdId}/today")
    public ResponseEntity<List<TaskDto>> getTodaysTasks(@PathVariable Long householdId) {
        return ResponseEntity.ok(taskService.getTodaysTasks(householdId));
    }

    @GetMapping("/household/{householdId}/pending")
    public ResponseEntity<List<TaskDto>> getPendingTasks(@PathVariable Long householdId) {
        return ResponseEntity.ok(taskService.getPendingTasks(householdId));
    }

    @PostMapping("/household/{householdId}")
    public ResponseEntity<TaskDto> createTask(
            @PathVariable Long householdId,
            @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.createTask(request, householdId));
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<TaskDto> completeTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.completeTask(taskId));
    }

    @PostMapping("/{taskId}/skip")
    public ResponseEntity<TaskDto> skipTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.skipTask(taskId));
    }

    @PostMapping("/{taskId}/move")
    public ResponseEntity<TaskDto> moveTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.moveTask(taskId));
    }

    @GetMapping("/household/{householdId}/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(@PathVariable Long householdId) {
        return ResponseEntity.ok(taskService.getLeaderboard(householdId));
    }
}
