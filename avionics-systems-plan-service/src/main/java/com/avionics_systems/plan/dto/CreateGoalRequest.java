package com.avionics_systems.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGoalRequest {

    @NotBlank(message = "Goal name is required")
    @Size(max = 255)
    private String name;

    private String description;

    private String status;

    private LocalDate targetDate;

    private UUID parentGoalId;

    private List<String> linkedEpicIds;

    private String color;

    private UUID ownerUserId;
}
