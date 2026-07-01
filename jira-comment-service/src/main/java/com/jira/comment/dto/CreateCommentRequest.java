package com.jira.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotNull(message = "Issue ID is required")
    private UUID issueId;

    @NotBlank(message = "Content is required")
    private String content;

    private UUID parentCommentId;

    @Builder.Default
    private Boolean internal = false;
}