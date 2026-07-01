package com.jira.workflow.exception;

public class TransitionConflictException extends RuntimeException {
    public TransitionConflictException(String message) {
        super(message);
    }
}
