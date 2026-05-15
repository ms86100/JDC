package com.jira.migration.service.clients.dto;

import lombok.*;

import java.util.List;

/**
 * Response DTO for paginated Audit Log operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedAuditLogResponse {

    private List<AuditLogResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}