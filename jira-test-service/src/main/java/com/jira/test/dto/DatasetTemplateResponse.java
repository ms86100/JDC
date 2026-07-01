package com.jira.test.dto;

import lombok.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetTemplateResponse {

    private String templateId;
    private String name;
    private String description;
    private String category; // TEST_DATA, CONFIG, LOCATIONS, USERS, PRODUCTS, CUSTOM
    private Boolean isBuiltIn;

    private List<String> columnNames;
    private List<String> columnTypes;
    private List<String> sampleData;

    private Integer typicalRowCount;
    private List<String> tags;
    private Map<String, String> metadata;

    // Template usage info
    private Integer usageCount;
    private Boolean isRecommended;
    private String difficultyLevel; // BEGINNER, INTERMEDIATE, ADVANCED
}