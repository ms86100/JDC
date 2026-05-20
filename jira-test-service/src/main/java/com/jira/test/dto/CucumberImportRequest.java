package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CucumberImportRequest {

    @NotBlank
    private UUID projectId;

    private String featureContent;

    private String featureFileName;

    private List<String> tags;

    private UUID testSetId;
}