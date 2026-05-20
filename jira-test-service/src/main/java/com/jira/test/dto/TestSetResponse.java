package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSetResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private String testType;
    private List<String> labels;
    private Integer testCount;
    private Boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}