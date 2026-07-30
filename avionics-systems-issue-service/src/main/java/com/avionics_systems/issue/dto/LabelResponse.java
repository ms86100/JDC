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
public class LabelResponse {
    private UUID id;
    private UUID issueId;
    private String name;
    private String color;
    private String description;
    private UUID createdBy;
    private LocalDateTime createdAt;
}