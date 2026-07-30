package com.avionics_systems.project.dto;

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
public class ProjectTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private String category;
    private String icon;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
}