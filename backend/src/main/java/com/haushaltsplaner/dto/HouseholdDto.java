package com.haushaltsplaner.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseholdDto {
    private Long id;
    private String name;
    private String inviteCode;
    private Integer memberCount;
}
