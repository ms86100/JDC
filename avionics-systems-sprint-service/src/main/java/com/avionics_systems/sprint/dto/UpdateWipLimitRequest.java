package com.avionics_systems.sprint.dto;

import lombok.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating WIP (Work In Progress) limits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWipLimitRequest {

    @NotNull(message = "Column ID is required")
    private java.util.UUID columnId;

    @Min(value = 0, message = "WIP limit must be non-negative")
    private Integer wipLimit;

    /**
     * If true, enables WIP limit enforcement. If false, disables it.
     */
    private Boolean enabled;
}