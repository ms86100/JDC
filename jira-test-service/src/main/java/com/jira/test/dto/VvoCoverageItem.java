package com.jira.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VvoCoverageItem {
    private UUID vvoId;
    private String issueKey;
    private String summary;
    private String status;
    private Integer vvoVersion;
    private String idDoors;
    private List<String> applicability;
    private int linkedTestCount;
    private String coverageStatus;
    private List<UUID> componentIds;
}
