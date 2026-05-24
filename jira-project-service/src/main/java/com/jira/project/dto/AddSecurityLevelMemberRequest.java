package com.jira.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddSecurityLevelMemberRequest {

    @NotBlank(message = "Member type is required")
    @Size(max = 20, message = "Member type must not exceed 20 characters")
    private String memberType; // "USER", "GROUP", "PROJECT_ROLE"

    @NotNull(message = "Member ID is required")
    private String memberId; // UUID as string for deserialization

    @Size(max = 100, message = "Group name must not exceed 100 characters")
    private String groupName; // Only for GROUP type
}