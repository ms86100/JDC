package com.avionics_systems.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeItemResponse {
    private UUID id;
    private UUID changeGroupId;
    private String fieldType;
    private String field;
    private String oldValue;
    private String oldString;
    private String newValue;
    private String newString;
    private LocalDateTime createdAt;
}