package com.avionics_systems.sprint.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddIssueToSprintRequest {
    private UUID issueId;
    private Integer orderIndex;
}
