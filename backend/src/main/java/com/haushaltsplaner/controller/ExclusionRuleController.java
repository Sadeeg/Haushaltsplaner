package com.haushaltsplaner.controller;

import com.haushaltsplaner.dto.ExclusionRuleDto;
import com.haushaltsplaner.service.ExclusionRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class ExclusionRuleController {

    private final ExclusionRuleService exclusionRuleService;

    @GetMapping("/household/{householdId}")
    public ResponseEntity<List<ExclusionRuleDto>> getRules(@PathVariable Long householdId) {
        return ResponseEntity.ok(exclusionRuleService.getRulesForHousehold(householdId));
    }

    @PostMapping("/household/{householdId}")
    public ResponseEntity<ExclusionRuleDto> createRule(
            @PathVariable Long householdId,
            @RequestBody ExclusionRuleDto dto) {
        return ResponseEntity.ok(exclusionRuleService.createRule(dto, householdId));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long ruleId) {
        exclusionRuleService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}
