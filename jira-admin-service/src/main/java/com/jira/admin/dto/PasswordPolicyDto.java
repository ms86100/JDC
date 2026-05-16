package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PasswordPolicyDto {
    private String id;
    private String name;
    private Integer minLength;
    private Integer maxLength;
    private Boolean requireUppercase;
    private Boolean requireLowercase;
    private Boolean requireDigit;
    private Boolean requireSpecialChar;
    private Integer passwordHistoryCount;
    private Integer expirationDays;
    private Boolean isActive;
}