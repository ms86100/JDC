package com.jira.test.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagRequest {
    @NotEmpty(message = "At least one tag is required")
    @Size(max = 20, message = "Cannot have more than 20 tags")
    private List<@Size(max = 50, message = "Tag cannot exceed 50 characters") String> tags;

    private UUID userId;
}
