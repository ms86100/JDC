package com.jira.issue.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalPageLinkRequest {
    @NotBlank
    private String url;
    private String title;
    private UUID applicationLinkId;
    private String pageId;
    private String spaceKey;
}
