package com.jira.comment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentRequest {

    @NotBlank(message = "Content is required")
    private String content;

    /**
     * Version for optimistic locking.
     * If provided, the update will fail with 409 Conflict if the entity
     * has been modified by another user since the version was read.
     */
    @Min(0)
    private Long version;
}