package com.jira.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptConsoleRequest {

    @NotBlank(message = "Script body is required")
    private String scriptBody;

    @NotBlank(message = "Script type is required")
    private String scriptType;

    private Map<String, Object> context;
}
