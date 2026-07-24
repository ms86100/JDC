package com.jira.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScriptRequest {

    @NotBlank(message = "Script name is required")
    private String name;

    private String description;

    @NotBlank(message = "Script type is required")
    private String scriptType;

    @NotBlank(message = "Script key is required")
    @Pattern(regexp = "^[a-z][a-z0-9-]{2,63}$",
            message = "Script key must be 3-64 chars, lowercase alphanumeric with dashes, starting with a letter")
    private String scriptKey;

    @NotBlank(message = "Script body is required")
    private String scriptBody;
}
