package com.jira.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineRequest {

    @NotNull(message = "Test ID is required")
    private UUID testId;

    private String status; // candidate, quarantined, investigation

    private String quarantineReason;

    private String triggerType; // auto_flaky, auto_failing, manual

    private Boolean autoRestoreEnabled;

    private Map<String, Object> autoRestoreConditions; // {passCount: 3, daysElapsed: 7}
}