package com.haushaltsplaner.service;

import com.haushaltsplaner.domain.Household;
import com.haushaltsplaner.domain.User;
import com.haushaltsplaner.dto.UserDto;
import com.haushaltsplaner.repository.HouseholdRepository;
import com.haushaltsplaner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;

    @Transactional(readOnly = true)
    public Optional<UserDto> findById(Long id) {
        return userRepository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findByUsername(String username) {
        return userRepository.findByUsername(username).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findByNextcloudId(String nextcloudId) {
        return userRepository.findByNextcloudId(nextcloudId).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findByTelegramChatId(Long telegramChatId) {
        return userRepository.findByTelegramChatId(telegramChatId).map(this::toDto);
    }

    /**
     * Creates or updates a user from OAuth login.
     * Auto-assigns to default household if user has none.
     */
    @Transactional
    public UserDto createOrUpdateFromOAuth(String username, String email, String nextcloudId, String displayName) {
        User user = userRepository.findByNextcloudId(nextcloudId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(username)
                            .email(email)
                            .nextcloudId(nextcloudId)
                            .displayName(displayName != null ? displayName : username)
                            .build();
                    
                    // Auto-assign to default household (first one or create new)
                    Household household = getOrCreateDefaultHousehold();
                    newUser.setHousehold(household);
                    
                    return newUser;
                });
        
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName != null ? displayName : username);
        
        return toDto(userRepository.save(user));
    }

    private Household getOrCreateDefaultHousehold() {
        List<Household> households = householdRepository.findAll();
        if (households.isEmpty()) {
            Household newHousehold = Household.builder()
                    .name("Haushalt")
                    .build();
            return householdRepository.save(newHousehold);
        }
        return households.get(0);
    }

    @Transactional
    public String generateTelegramVerificationCode(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String code = generateRandomCode();
        user.setTelegramVerificationCode(code);
        userRepository.save(user);
        
        return code;
    }

    @Transactional
    public UserDto linkTelegramAccount(String verificationCode, Long telegramChatId) {
        User user = userRepository.findByTelegramVerificationCode(verificationCode)
                .orElseThrow(() -> new RuntimeException("Invalid verification code"));
        
        user.setTelegramChatId(telegramChatId);
        user.setTelegramVerificationCode(null);
        
        return toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .hasTelegram(user.getTelegramChatId() != null)
                .householdId(user.getHousehold() != null ? user.getHousehold().getId() : null)
                .householdName(user.getHousehold() != null ? user.getHousehold().getName() : null)
                .telegramChatId(user.getTelegramChatId())
                .build();
    }

    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
            if (i < 2) code.append("-");
        }
        code.append(chars.charAt((int) (Math.random() * chars.length())));
        return code.toString();
    }
}
