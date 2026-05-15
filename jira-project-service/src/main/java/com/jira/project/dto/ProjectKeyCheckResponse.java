package com.jira.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectKeyCheckResponse {
    private String projectKey;
    private boolean valid;
    private boolean available;
    private String message;

    public ProjectKeyCheckResponse(String key, boolean valid, boolean available) {
        this.projectKey = key;
        this.valid = valid;
        this.available = available;
        if (!valid) {
            this.message = "Project key must be 2-10 uppercase alphanumeric characters, starting with a letter";
        } else if (!available) {
            this.message = "Project key is already in use";
        } else {
            this.message = "Project key is available";
        }
    }
}