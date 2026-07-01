package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationLinkResponse {
    private String id;
    private String name;
    private String url;
    private String applicationType;
    private String direction;
    private String status;
    private boolean primary;
}
