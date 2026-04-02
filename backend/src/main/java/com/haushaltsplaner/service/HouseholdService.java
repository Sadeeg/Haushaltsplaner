package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.Household;
import com.haushaltsplaner.domain.User;
import com.haushaltsplaner.repository.HouseholdRepository;
import com.haushaltsplaner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;

    @Transactional
    public Household createHousehold(String name) {
        Household household = Household.builder()
                .name(name)
                .inviteCode(generateInviteCode())
                .build();
        return householdRepository.save(household);
    }

    @Transactional
    public Household joinHousehold(Long userId, String inviteCode) {
        Household household = householdRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Household not found with this invite code"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setHousehold(household);
        household.getMembers().add(user);
        userRepository.save(user);
        
        return household;
    }

    @Transactional(readOnly = true)
    public Household getHousehold(Long householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found"));
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
