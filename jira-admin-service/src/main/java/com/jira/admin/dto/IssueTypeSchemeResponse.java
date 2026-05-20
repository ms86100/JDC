package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTypeSchemeResponse {
    private String id;
    private String name;
    private String description;
    private String defaultIssueType;
    private List<String> issueTypeIdList;
    private Integer projectCount;
    private Boolean isDefault;
}
