package com.haushaltsplaner.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramLinkRequest {
    private String verificationCode;
}
