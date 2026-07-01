package com.jira.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLegalHoldRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private UUID legalMatterId;
    private String matterReference;

    @NotNull(message = "Hold type is required")
    private String holdType;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean autoExtend;
    private Integer extensionPeriodDays;
    private String scope;
    private String preservationInstructions;
    private String[] dataCategories;
    private UUID[] projectIds;
    private String legalBasis;
    private Boolean isCritical;
    private String metadata;

    @NotNull(message = "Custodian IDs are required")
    private UUID[] custodianIds;
}