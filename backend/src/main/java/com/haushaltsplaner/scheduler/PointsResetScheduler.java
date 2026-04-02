package com.haushaltsplaner.scheduler;

import com.haushaltsplaner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointsResetScheduler {

    private final UserRepository userRepository;

    /**
     * Runs on the 1st of every month at 00:01 to reset monthly points.
     */
    @Scheduled(cron = "0 1 0 1 * *") // 00:01 on the 1st of every month
    @Transactional
    public void resetMonthlyPoints() {
        log.info("Starting scheduled monthly points reset...");
        
        LocalDateTime now = LocalDateTime.now();
        
        userRepository.findAll().forEach(user -> {
            user.setMonthlyPoints(0);
            user.setLastPointsReset(now);
            userRepository.save(user);
        });
        
        log.info("Completed monthly points reset for all users");
    }
}
