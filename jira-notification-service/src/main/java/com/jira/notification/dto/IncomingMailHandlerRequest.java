package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingMailHandlerRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Builder.Default
    private String serverType = "IMAP";

    @NotBlank(message = "Host is required")
    private String host;

    @NotNull(message = "Port is required")
    @Builder.Default
    private Integer port = 993;

    @Builder.Default
    private Boolean useSsl = true;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @Builder.Default
    private String folder = "INBOX";

    @Builder.Default
    private String handlerType = "CREATE_ISSUE";

    private UUID projectId;

    private UUID issueTypeId;

    private UUID defaultReporterId;

    @Builder.Default
    private Boolean isEnabled = true;

    @Builder.Default
    private Integer pollIntervalMinutes = 5;
}
