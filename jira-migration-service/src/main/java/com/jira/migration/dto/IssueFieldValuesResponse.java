package com.jira.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueFieldValuesResponse {
    private UUID issueId;
    private String issueKey;
    private Map<String, Object> standardFields;
    private Map<String, Object> customFields;
    private List<FieldValueResponse> allFieldValues;
    private List<String> validationErrors;
}