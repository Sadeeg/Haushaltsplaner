package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.Household;
import com.haushaltsplaner.domain.User;
import com.haushaltsplaner.dto.UserDto;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HouseholdRepository householdRepository;

    @InjectMocks
    private UserService userService;

    private Household household;
    private User user;

    @BeforeEach
    void setUp() {
        household = Household.builder()
                .id(1L)
                .name("Test Household")
                .members(new HashSet<>())
                .build();

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .nextcloudId("nc123")
                .monthlyPoints(0)
                .household(household)
                .build();

        household.getMembers().add(user);
    }

    @Test
    @DisplayName("Should create new user from OAuth when user doesn't exist")
    void testCreateOrUpdateFromOAuth_CreatesNewUser() {
        when(userRepository.findByNextcloudId("nc123")).thenReturn(Optional.empty());
        when(householdRepository.findAll()).thenReturn(List.of(household));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserDto result = userService.createOrUpdateFromOAuth("newuser", "new@example.com", "nc123", "New User");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("New User", result.getDisplayName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update existing user from OAuth when user exists")
    void testCreateOrUpdateFromOAuth_UpdatesExistingUser() {
        when(userRepository.findByNextcloudId("nc123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.createOrUpdateFromOAuth("updateduser", "updated@example.com", "nc123", "Updated Name");

        assertEquals("updateduser", result.getUsername());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("Updated Name", result.getDisplayName());
    }

    @Test
    @DisplayName("Should assign default household to new OAuth user")
    void testCreateOrUpdateFromOAuth_AssignsDefaultHousehold() {
        when(userRepository.findByNextcloudId("nc123")).thenReturn(Optional.empty());
        when(householdRepository.findAll()).thenReturn(List.of(household));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.createOrUpdateFromOAuth("newuser", "new@example.com", "nc123", "New User");

        assertEquals(1L, result.getHouseholdId());
        assertEquals("Test Household", result.getHouseholdName());
    }

    @Test
    @DisplayName("Should update chat ID when linking Telegram account")
    void testLinkTelegramAccount_UpdatesChatId() {
        user.setTelegramVerificationCode("ABC-12");
        Long newChatId = 123456789L;

        when(userRepository.findByTelegramVerificationCode("ABC-12")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.linkTelegramAccount("ABC-12", newChatId);

        assertEquals(newChatId, user.getTelegramChatId());
        assertNull(user.getTelegramVerificationCode());
    }

    @Test
    @DisplayName("Should find user by ID")
    void testFindById_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<UserDto> result = userService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    @DisplayName("Should find user by username")
    void testFindByUsername_ReturnsUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Optional<UserDto> result = userService.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    @DisplayName("Should find user by nextcloud ID")
    void testFindByNextcloudId_ReturnsUser() {
        when(userRepository.findByNextcloudId("nc123")).thenReturn(Optional.of(user));

        Optional<UserDto> result = userService.findByNextcloudId("nc123");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    @DisplayName("Should find user by Telegram chat ID")
    void testFindByTelegramChatId_ReturnsUser() {
        user.setTelegramChatId(123456789L);
        when(userRepository.findByTelegramChatId(123456789L)).thenReturn(Optional.of(user));

        Optional<UserDto> result = userService.findByTelegramChatId(123456789L);

        assertTrue(result.isPresent());
        assertTrue(result.get().getHasTelegram());
    }

    @Test
    @DisplayName("Should generate verification code for Telegram linking")
    void testGenerateTelegramVerificationCode_ReturnsCode() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        String code = userService.generateTelegramVerificationCode(1L);

        assertNotNull(code);
        assertTrue(code.contains("-"));
        assertTrue(code.length() >= 6); // Format: X-X-X (6-8 chars depending on dashes)
        assertNotNull(user.getTelegramVerificationCode());
    }

    @Test
    @DisplayName("Should throw exception when linking Telegram with invalid code")
    void testLinkTelegramAccount_WithInvalidCode_ThrowsException() {
        when(userRepository.findByTelegramVerificationCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            userService.linkTelegramAccount("INVALID", 123456789L)
        );
    }

    @Test
    @DisplayName("Should return all users")
    void testGetAllUsers_ReturnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> users = userService.getAllUsers();

        assertEquals(1, users.size());
    }

    @Test
    @DisplayName("Should create default household when none exists")
    void testCreateOrUpdateFromOAuth_CreatesDefaultHouseholdWhenNoneExists() {
        when(userRepository.findByNextcloudId("nc123")).thenReturn(Optional.empty());
        when(householdRepository.findAll()).thenReturn(Collections.emptyList());
        when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> {
            Household h = invocation.getArgument(0);
            h.setId(1L);
            return h;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.createOrUpdateFromOAuth("newuser", "new@example.com", "nc123", "New User");

        verify(householdRepository).save(any(Household.class));
    }
}
