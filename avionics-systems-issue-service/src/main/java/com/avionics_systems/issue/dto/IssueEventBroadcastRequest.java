package com.avionics_systems.issue.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class IssueEventBroadcastRequest {
    private String type;
    private UUID issueId;
    private UUID projectId;
}
