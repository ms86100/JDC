package com.avionics_systems.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionStatusConfigResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String displayName;
    private String color;
    private String icon;
    private Boolean isPass;
    private Boolean isFail;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
