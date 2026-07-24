package com.jira.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptConsoleResponse {

    private boolean success;
    private Object result;
    private String errorMessage;
    private long executionMs;
    private String consoleOutput;
}
