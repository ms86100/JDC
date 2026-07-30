package com.avionics_systems.workflow.exception;

public class TransitionConflictException extends RuntimeException {
    public TransitionConflictException(String message) {
        super(message);
    }
}
