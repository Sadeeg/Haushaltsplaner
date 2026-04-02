package com.haushaltsplaner.integration;

import com.haushaltsplaner.domain.Household;
import com.haushaltsplaner.domain.User;
import com.haushaltsplaner.repository.HouseholdRepository;
import com.haushaltsplaner.repository.UserRepository;
import com.haushaltsplaner.service.HouseholdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class HouseholdServiceIntegrationTest {

    @Autowired
    private HouseholdService householdService;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .monthlyPoints(0)
                .household(null)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should create household and return invite code")
    void testCreateHousehold_ReturnsInviteCode() {
        String householdName = "Test Household";
        
        Household created = householdService.createHousehold(householdName);
        
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(householdName, created.getName());
        assertNotNull(created.getInviteCode());
        assertFalse(created.getInviteCode().isEmpty());
        assertEquals(8, created.getInviteCode().length()); // UUID.substring(0,8) = 8 chars
    }

    @Test
    @DisplayName("Should allow user to join household with valid invite code")
    void testJoinHousehold_WithValidCode_AddsUserToHousehold() {
        // Create a household with invite code
        Household household = householdService.createHousehold("Join Test Household");
        String inviteCode = household.getInviteCode();
        
        // User joins the household
        Household joinedHousehold = householdService.joinHousehold(testUser.getId(), inviteCode);
        
        // Verify the user is now part of the household
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertNotNull(updatedUser.getHousehold());
        assertEquals(household.getId(), updatedUser.getHousehold().getId());
        assertEquals(1, joinedHousehold.getMembers().size());
    }

    @Test
    @DisplayName("Should throw exception when joining with invalid invite code")
    void testJoinHousehold_WithInvalidCode_ThrowsException() {
        assertThrows(RuntimeException.class, () -> 
            householdService.joinHousehold(testUser.getId(), "INVALID")
        );
    }

    @Test
    @DisplayName("Should return correct member count after adding users")
    void testGetHouseholdInfo_ReturnsCorrectMemberCount() {
        // Create household and add user
        Household household = householdService.createHousehold("Member Count Test");
        householdService.joinHousehold(testUser.getId(), household.getInviteCode());
        
        // Add more users
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .displayName("User 2")
                .monthlyPoints(0)
                .build();
        user2 = userRepository.save(user2);
        
        User user3 = User.builder()
                .username("user3")
                .email("user3@example.com")
                .displayName("User 3")
                .monthlyPoints(0)
                .build();
        user3 = userRepository.save(user3);
        
        householdService.joinHousehold(user2.getId(), household.getInviteCode());
        householdService.joinHousehold(user3.getId(), household.getInviteCode());
        
        // Fetch household and verify member count
        Household fetchedHousehold = householdService.getHousehold(household.getId());
        assertEquals(3, fetchedHousehold.getMembers().size());
    }

    @Test
    @DisplayName("Should create multiple households with unique invite codes")
    void testCreateHousehold_UniqueInviteCodes() {
        Household household1 = householdService.createHousehold("Household 1");
        Household household2 = householdService.createHousehold("Household 2");
        
        assertNotEquals(household1.getInviteCode(), household2.getInviteCode());
    }
}
