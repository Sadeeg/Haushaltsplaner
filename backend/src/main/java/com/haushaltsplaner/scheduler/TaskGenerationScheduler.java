package com.haushaltsplaner.scheduler;

import com.haushaltsplaner.service.RotationService;
import com.haushaltsplaner.repository.HouseholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskGenerationScheduler {

    private final RotationService rotationService;
    private final HouseholdRepository householdRepository;

    /**
     * Runs every day at 00:05 to generate tasks for the next 14 days.
     */
    @Scheduled(cron = "0 5 0 * * *") // 00:05 every day
    public void generateTasksForNext14Days() {
        log.info("Starting scheduled task generation for next 14 days...");
        
        // Get all household IDs
        householdRepository.findAll().forEach(household -> {
            Long householdId = household.getId();
            
            // Generate tasks for today + next 14 days
            for (int i = 0; i <= 14; i++) {
                LocalDate date = LocalDate.now().plusDays(i);
                try {
                    rotationService.generateTasksForDate(householdId, date);
                    log.debug("Generated tasks for household {} on {}", householdId, date);
                } catch (Exception e) {
                    log.error("Failed to generate tasks for household {} on {}: {}", 
                            householdId, date, e.getMessage());
                }
            }
            
            log.info("Completed task generation for household {} ({} days)", householdId, 15);
        });
        
        log.info("Completed scheduled task generation for all households");
    }
}
