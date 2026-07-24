package com.jira.workflow.engine.script;

public record ScriptResult(
        boolean success,
        Object value,
        String errorMessage,
        long executionMs,
        String consoleOutput
) {
    public static ScriptResult success(Object value, long ms, String consoleOutput) {
        return new ScriptResult(true, value, null, ms, consoleOutput);
    }

    public static ScriptResult error(String message, long ms, String consoleOutput) {
        return new ScriptResult(false, null, message, ms, consoleOutput);
    }

    public static ScriptResult success(Object value, long ms) {
        return new ScriptResult(true, value, null, ms, null);
    }

    public static ScriptResult error(String message, long ms) {
        return new ScriptResult(false, null, message, ms, null);
    }
}
