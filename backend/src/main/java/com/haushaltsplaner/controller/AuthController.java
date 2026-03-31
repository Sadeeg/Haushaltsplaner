package com.haushaltsplaner.controller;

import com.haushaltsplaner.dto.UserDto;
import com.haushaltsplaner.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/oauth/callback")
    public ResponseEntity<UserDto> handleOAuthCallback(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String email = payload.get("email");
        String nextcloudId = payload.get("nextcloudId");
        String displayName = payload.get("displayName");

        UserDto user = userService.createOrUpdateFromOAuth(username, email, nextcloudId, displayName);
        return ResponseEntity.ok(user);
    }
}
