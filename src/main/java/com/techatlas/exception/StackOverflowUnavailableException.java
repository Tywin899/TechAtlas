package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class StackOverflowUnavailableException extends TechAtlasException {
    public StackOverflowUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public StackOverflowUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
