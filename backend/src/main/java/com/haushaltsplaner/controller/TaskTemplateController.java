package com.haushaltsplaner.controller;

import com.haushaltsplaner.dto.TaskTemplateDto;
import com.haushaltsplaner.service.TaskTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TaskTemplateController {

    private final TaskTemplateService taskTemplateService;

    @GetMapping("/household/{householdId}")
    public ResponseEntity<List<TaskTemplateDto>> getTemplates(@PathVariable Long householdId) {
        return ResponseEntity.ok(taskTemplateService.getTemplatesForHousehold(householdId));
    }

    @PostMapping("/household/{householdId}")
    public ResponseEntity<TaskTemplateDto> createTemplate(
            @PathVariable Long householdId,
            @RequestBody TaskTemplateDto dto) {
        return ResponseEntity.ok(taskTemplateService.createTemplate(dto, householdId));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        taskTemplateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }
}
