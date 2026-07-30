package com.avionics_systems.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
