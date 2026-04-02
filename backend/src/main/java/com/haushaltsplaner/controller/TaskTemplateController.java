package com.haushaltsplaner.controller;

import com.haushaltsplaner.dto.TaskTemplateDto;
import com.haushaltsplaner.service.RotationService;
import com.haushaltsplaner.service.TaskTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TaskTemplateController {

    private final TaskTemplateService taskTemplateService;
    private final RotationService rotationService;

    @GetMapping("/household/{householdId}")
    public ResponseEntity<List<TaskTemplateDto>> getTemplates(@PathVariable Long householdId) {
        return ResponseEntity.ok(taskTemplateService.getTemplatesForHousehold(householdId));
    }

    @PostMapping("/household/{householdId}")
    public ResponseEntity<TaskTemplateDto> createTemplate(
            @PathVariable Long householdId,
            @RequestBody TaskTemplateDto dto) {
        TaskTemplateDto created = taskTemplateService.createTemplate(dto, householdId);
        
        // Generate tasks for today and next 14 days for this new template
        for (int i = 0; i <= 14; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            try {
                rotationService.generateTasksForDate(householdId, date);
            } catch (Exception e) {
                // Log but continue
            }
        }
        
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        taskTemplateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }
}
