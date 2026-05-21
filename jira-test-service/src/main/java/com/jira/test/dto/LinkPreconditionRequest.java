package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkPreconditionRequest {
    private UUID testId;
    private Integer stepOrder;
    private String notes;
}