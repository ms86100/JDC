package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JunitImportRequest {

    private UUID projectId;

    private String xmlContent;

    private String ciSource;

    private String ciBuildUrl;

    private String ciJobName;

    private String ciBuildNumber;

    private String branch;

    private String commitSha;

    private UUID testSetId;
}