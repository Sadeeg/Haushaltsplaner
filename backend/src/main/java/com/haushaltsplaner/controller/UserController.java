package com.haushaltsplaner.controller;

import com.haushaltsplaner.dto.UserDto;
import com.haushaltsplaner.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@RequestParam String username) {
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
