package com.avionics_systems.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingMailHandlerResponse {

    private UUID id;
    private String name;
    private String serverType;
    private String host;
    private Integer port;
    private Boolean useSsl;
    private String username;
    private String folder;
    private String handlerType;
    private UUID projectId;
    private UUID issueTypeId;
    private UUID defaultReporterId;
    private Boolean isEnabled;
    private Integer pollIntervalMinutes;
    private OffsetDateTime lastPollAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
