package com.haushaltsplaner.controller;

import com.haushaltsplaner.domain.Household;
import com.haushaltsplaner.domain.User;
import com.haushaltsplaner.dto.HouseholdDto;
import com.haushaltsplaner.repository.HouseholdRepository;
import com.haushaltsplaner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@RequestMapping("/api/households")
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<HouseholdDto> createHousehold(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateHouseholdRequest request) {
        String nextcloudId = jwt.getSubject();
        User user = userRepository.findByNextcloudId(nextcloudId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user already has a household
        if (user.getHousehold() != null) {
            return ResponseEntity.badRequest().build();
        }

        // Generate invite code
        String inviteCode = generateInviteCode();

        Household household = Household.builder()
                .name(request.getName())
                .inviteCode(inviteCode)
                .build();
        household = householdRepository.save(household);

        // Add user to household
        user.setHousehold(household);
        userRepository.save(user);

        return ResponseEntity.ok(toDto(household));
    }

    @PostMapping("/join")
    public ResponseEntity<HouseholdDto> joinHousehold(@AuthenticationPrincipal Jwt jwt, @RequestBody JoinHouseholdRequest request) {
        String nextcloudId = jwt.getSubject();
        User user = userRepository.findByNextcloudId(nextcloudId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user already has a household
        if (user.getHousehold() != null) {
            return ResponseEntity.badRequest().build();
        }

        // Find household by invite code
        Household household = householdRepository.findByInviteCode(request.getInviteCode().toUpperCase())
                .orElse(null);

        if (household == null) {
            return ResponseEntity.notFound().build();
        }

        // Add user to household
        user.setHousehold(household);
        userRepository.save(user);

        return ResponseEntity.ok(toDto(household));
    }

    @GetMapping("/{householdId}")
    public ResponseEntity<HouseholdDto> getHouseholdInfo(@PathVariable Long householdId) {
        Household household = householdRepository.findById(householdId)
                .orElse(null);

        if (household == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toDto(household));
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private HouseholdDto toDto(Household household) {
        int memberCount = household.getMembers() != null ? household.getMembers().size() : 0;
        return HouseholdDto.builder()
                .id(household.getId())
                .name(household.getName())
                .inviteCode(household.getInviteCode())
                .memberCount(memberCount)
                .build();
    }

    public static class CreateHouseholdRequest {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class JoinHouseholdRequest {
        private String inviteCode;

        public String getInviteCode() { return inviteCode; }
        public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    }
}
