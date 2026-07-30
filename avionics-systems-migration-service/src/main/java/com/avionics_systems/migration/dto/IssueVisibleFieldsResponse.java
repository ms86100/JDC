package com.avionics_systems.migration.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueVisibleFieldsResponse {
    private UUID issueId;
    private String issueKey;
    private UUID projectId;
    private UUID issueTypeId;
    private String screenType;
    private List<VisibleFieldResponse> fields;
    private int totalCount;
}
