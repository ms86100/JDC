package com.avionics_systems.issue.dto;

import lombok.*;
import java.util.UUID;

/**
 * Webhook response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {

    private Boolean success;
    private String message;
    private UUID batchId;
    private Integer totalTests;
    private Integer passed;
    private Integer failed;
    private Integer skipped;
    private String error;
}