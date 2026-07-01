package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepResultUpdateRequest {

    @NotBlank
    private String status;

    private String actualResult;

    private List<String> evidenceUrls;

    private String defectKey;

    private String comment;
}