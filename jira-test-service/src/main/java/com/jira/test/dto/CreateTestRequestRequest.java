package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestRequestRequest {

    @NotNull
    private UUID projectId;

    @NotBlank
    private String summary;

    private String description;
    private String requestType;
    private UUID assigneeId;
    private UUID fixVersionId;
    private List<String> labels;
}
