package com.avionics_systems.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueueResponse {

    private UUID id;
    private String recipientEmail;
    private String subject;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private OffsetDateTime createdAt;
    private OffsetDateTime sentAt;
    private OffsetDateTime nextRetryAt;
}
