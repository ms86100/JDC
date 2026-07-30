package com.avionics_systems.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueLinkWorkflowContextResponse {
    private UUID linkId;
    private String linkType;
    private String direction;
    private UUID linkedIssueId;
    private String linkedIssueKey;
    private UUID statusId;
    private String statusName;
}
