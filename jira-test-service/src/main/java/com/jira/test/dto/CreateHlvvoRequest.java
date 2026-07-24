package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHlvvoRequest {

    @NotNull
    private UUID projectId;

    @NotBlank
    private String summary;

    private String description;
    private LocalDate targetDate;
    private String airbusReference;
    private UUID assigneeId;
    private String specificationReference;
    private List<UUID> componentIds;
    private String ptsLink;
    private String mfclLink;
    private UUID fixVersionId;
    private List<String> labels;
}
