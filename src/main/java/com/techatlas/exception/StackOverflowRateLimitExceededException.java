package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class StackOverflowRateLimitExceededException extends TechAtlasException {
    public StackOverflowRateLimitExceededException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    public StackOverflowRateLimitExceededException(String message, Throwable cause) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, cause);
    }
}
