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

    @NotNull(message = "{validation.issue.id.required}")
    private UUID issueId;

    @NotBlank(message = "{validation.content.required}")
    private String content;

    private UUID parentCommentId;

    @Builder.Default
    private Boolean internal = false;
}