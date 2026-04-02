package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.Household;
import com.haushaltsplaner.domain.User;
import com.haushaltsplaner.repository.HouseholdRepository;
import com.haushaltsplaner.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HouseholdServiceTest {

    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HouseholdService householdService;

    private Household household;
    private User user;

    @BeforeEach
    void setUp() {
        household = Household.builder()
                .id(1L)
                .name("Test Household")
                .inviteCode("ABC12345")
                .members(new HashSet<>())
                .build();

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .household(null)
                .assignedTasks(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Should generate invite code when creating household")
    void testCreateHousehold_GeneratesInviteCode() {
        when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> {
            Household h = invocation.getArgument(0);
            h.setId(1L);
            return h;
        });

        Household result = householdService.createHousehold("New Household");

        assertNotNull(result);
        assertEquals("New Household", result.getName());
        assertNotNull(result.getInviteCode());
        assertEquals(8, result.getInviteCode().length());
        verify(householdRepository).save(any(Household.class));
    }

    @Test
    @DisplayName("Should add user to household with valid invite code")
    void testJoinHousehold_WithValidCode() {
        when(householdRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(household));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        Household result = householdService.joinHousehold(1L, "ABC12345");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ABC12345", result.getInviteCode());
        verify(userRepository).save(user);
        assertEquals(household, user.getHousehold());
    }

    @Test
    @DisplayName("Should throw exception when joining with invalid invite code")
    void testJoinHousehold_WithInvalidCode_ReturnsNotFound() {
        when(householdRepository.findByInviteCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
            householdService.joinHousehold(1L, "INVALID")
        );
    }

    @Test
    @DisplayName("Should throw exception when user not found during join")
    void testJoinHousehold_UserNotFound() {
        when(householdRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(household));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
            householdService.joinHousehold(999L, "ABC12345")
        );
    }

    @Test
    @DisplayName("Should return household by ID")
    void testGetHousehold_ReturnsHousehold() {
        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));

        Household result = householdService.getHousehold(1L);

        assertNotNull(result);
        assertEquals("Test Household", result.getName());
    }

    @Test
    @DisplayName("Should throw exception when household not found")
    void testGetHousehold_NotFound() {
        when(householdRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
            householdService.getHousehold(999L)
        );
    }

    @Test
    @DisplayName("Should add user to household members set when joining")
    void testJoinHousehold_AddsUserToMembers() {
        when(householdRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(household));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        householdService.joinHousehold(1L, "ABC12345");

        assertTrue(household.getMembers().contains(user));
    }
}
