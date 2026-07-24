package com.jira.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaselineTagRequest {

    @NotNull
    private UUID projectId;

    @NotNull
    private UUID fixVersionId;

    @NotNull
    private List<UUID> vvoIds;
}
