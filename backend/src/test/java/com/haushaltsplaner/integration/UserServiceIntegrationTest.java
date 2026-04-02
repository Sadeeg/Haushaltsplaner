package com.haushaltsplaner.integration;

import com.haushaltsplaner.domain.Household;
import com.haushaltsplaner.domain.User;
import com.haushaltsplaner.dto.UserDto;
import com.haushaltsplaner.repository.HouseholdRepository;
import com.haushaltsplaner.repository.UserRepository;
import com.haushaltsplaner.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Test
    @DisplayName("Should create user with household if user doesn't exist")
    void testGetCurrentUser_CreatesUserIfNotExists() {
        String nextcloudId = "nc-new-user-123";
        
        // Verify user doesn't exist
        Optional<User> before = userRepository.findByNextcloudId(nextcloudId);
        assertTrue(before.isEmpty());
        
        // Create user via OAuth flow
        UserDto created = userService.createOrUpdateFromOAuth(
            "newuser", "new@example.com", nextcloudId, "New User"
        );
        
        // Verify user was created
        assertNotNull(created);
        assertEquals("newuser", created.getUsername());
        assertEquals("new@example.com", created.getEmail());
        assertEquals("New User", created.getDisplayName());
        
        // Verify user has a household assigned
        assertNotNull(created.getHouseholdId());
        assertNotNull(created.getHouseholdName());
    }

    @Test
    @DisplayName("Should return existing user without creating new one")
    void testGetCurrentUser_ReturnsExistingUser() {
        // First, create a user manually
        User existingUser = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .displayName("Existing User")
                .nextcloudId("nc-existing-123")
                .monthlyPoints(0)
                .build();
        
        Household household = householdRepository.save(Household.builder()
                .name("Existing User Household")
                .build());
        existingUser.setHousehold(household);
        existingUser = userRepository.save(existingUser);
        
        // Now try to "login" with same nextcloud ID
        UserDto result = userService.createOrUpdateFromOAuth(
            "existinguser", "existing@example.com", "nc-existing-123", "Existing User"
        );
        
        // Should return the same user
        assertEquals(existingUser.getId(), result.getId());
        assertEquals("existinguser", result.getUsername());
        
        // Verify only one user exists
        assertEquals(1, userRepository.findAll().stream()
                .filter(u -> u.getNextcloudId().equals("nc-existing-123"))
                .count());
    }

    @Test
    @DisplayName("Should have household assigned after user creation")
    void testUserHasHouseholdAfterCreation() {
        String nextcloudId = "nc-household-test-123";
        
        // Create user via OAuth
        UserDto user = userService.createOrUpdateFromOAuth(
            "householduser", "household@example.com", nextcloudId, "Household User"
        );
        
        // Verify household assignment
        assertNotNull(user.getHouseholdId());
        assertNotNull(user.getHouseholdName());
        
        // Fetch the household and verify it exists
        Household household = householdRepository.findById(user.getHouseholdId()).orElseThrow();
        assertNotNull(household);
        
        // Verify the user exists in the database with the correct household
        User createdUser = userRepository.findByNextcloudId(nextcloudId).orElseThrow();
        assertNotNull(createdUser.getHousehold());
        assertEquals(household.getId(), createdUser.getHousehold().getId());
    }

    @Test
    @DisplayName("Should create new household when no household exists")
    void testCreateUser_CreatesNewHouseholdWhenNoneExists() {
        // Ensure no households exist
        householdRepository.deleteAll();
        
        String nextcloudId = "nc-no-household-123";
        UserDto user = userService.createOrUpdateFromOAuth(
            "newhouseholduser", "newhousehold@example.com", nextcloudId, "New Household User"
        );
        
        // Verify a new household was created
        assertEquals(1, householdRepository.count());
        
        // Verify user is in the new household
        Household household = householdRepository.findAll().get(0);
        assertEquals(user.getHouseholdId(), household.getId());
    }

    @Test
    @DisplayName("Should link Telegram account successfully")
    void testLinkTelegramAccount() {
        // Create user
        User user = User.builder()
                .username("telegramuser")
                .email("telegram@example.com")
                .displayName("Telegram User")
                .nextcloudId("nc-telegram-123")
                .monthlyPoints(0)
                .build();
        user = userRepository.save(user);
        
        // Generate verification code
        String code = userService.generateTelegramVerificationCode(user.getId());
        assertNotNull(code);
        
        // Link Telegram account
        UserDto linked = userService.linkTelegramAccount(code, 123456789L);
        
        assertTrue(linked.getHasTelegram());
        assertEquals(123456789L, linked.getTelegramChatId());
    }
}
