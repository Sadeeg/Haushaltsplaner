package com.haushaltsplaner.controller;

import com.haushaltsplaner.dto.UserDto;
import com.haushaltsplaner.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get current user from JWT token.
     * Auto-creates user if they don't exist yet (first login).
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }

        // Extract user info from JWT claims
        String subject = jwt.getSubject();                    // Nextcloud user ID (sub claim)
        String username = jwt.getClaimAsString("preferred_username");  // may be null
        String email = jwt.getClaimAsString("email");        // may be null
        String displayName = jwt.getClaimAsString("name");   // may be null

        // Fallbacks
        if (username == null || username.isEmpty()) {
            username = subject;
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = username;
        }

        // Auto-create or update user
        UserDto user = userService.createOrUpdateFromOAuth(username, email, subject, displayName);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/{id}/telegram/generate-code")
    public ResponseEntity<String> generateTelegramCode(@PathVariable Long id) {
        try {
            String code = userService.generateTelegramVerificationCode(id);
            return ResponseEntity.ok(code);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/telegram/link")
    public ResponseEntity<UserDto> linkTelegram(@RequestParam String code, @RequestParam Long telegramChatId) {
        try {
            return ResponseEntity.ok(userService.linkTelegramAccount(code, telegramChatId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
