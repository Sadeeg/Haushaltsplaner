package com.haushaltsplaner.dto;

import com.haushaltsplaner.domain.TaskFrequency;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private Boolean hasTelegram;
    private Long householdId;
    private String householdName;
    private Long telegramChatId;
}
