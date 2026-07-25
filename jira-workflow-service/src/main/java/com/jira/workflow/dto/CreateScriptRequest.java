package com.jira.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScriptRequest {

    @NotBlank(message = "{validation.script.name.required}")
    @Size(max = 255, message = "Script name must be 255 characters or less")
    private String name;

    @Size(max = 2000, message = "Description must be 2000 characters or less")
    private String description;

    @NotBlank(message = "{validation.script.type.required}")
    @Pattern(regexp = "^(CONDITION|VALIDATOR|POST_FUNCTION|LISTENER|FIELD_BEHAVIOR|CALCULATED_FIELD|CONSOLE|SCHEDULED|LIBRARY)$",
            message = "Script type must be CONDITION, VALIDATOR, POST_FUNCTION, LISTENER, FIELD_BEHAVIOR, CALCULATED_FIELD, CONSOLE, SCHEDULED, or LIBRARY")
    private String scriptType;

    @NotBlank(message = "{validation.script.key.required}")
    @Pattern(regexp = "^[a-z][a-z0-9-]{2,63}$",
            message = "Script key must be 3-64 chars, lowercase alphanumeric with dashes, starting with a letter")
    private String scriptKey;

    @NotBlank(message = "{validation.script.body.required}")
    @Size(max = 65536, message = "Script body must be 64KB or less")
    private String scriptBody;
}
