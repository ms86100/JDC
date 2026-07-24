package com.jira.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VvoTransferRequest {

    @NotNull
    private UUID sourceProjectId;

    @NotNull
    private UUID targetProjectId;

    @NotNull
    private UUID fixVersionId;

    private boolean previewOnly;
}
