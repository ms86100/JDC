package com.avionics_systems.issue.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVersionRequest {

    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate releaseDate;
    private Integer sortOrder;
    private Boolean isReleased;
    private Boolean isArchived;
}