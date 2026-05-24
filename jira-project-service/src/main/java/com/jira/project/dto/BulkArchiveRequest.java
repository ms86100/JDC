package com.jira.project.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkArchiveRequest {

    private List<UUID> projectIds;
}