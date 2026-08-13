package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class StackOverflowMalformedResponseException extends TechAtlasException {
    public StackOverflowMalformedResponseException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public StackOverflowMalformedResponseException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
