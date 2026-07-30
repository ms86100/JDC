package com.avionics_systems.sprint.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResult {
    private String issueKey;
    private Boolean success;
    private String message;
    private String errorCode;
}