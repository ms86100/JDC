package com.avionics_systems.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddSecurityLevelMemberRequest {

    @NotBlank(message = "{validation.member.type.required}")
    @Size(max = 20, message = "{validation.member.type.max}")
    private String memberType; // "USER", "GROUP", "PROJECT_ROLE"

    @NotNull(message = "{validation.member.id.required}")
    private String memberId; // UUID as string for deserialization

    @Size(max = 100, message = "{validation.group.name.max}")
    private String groupName; // Only for GROUP type
}