package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class TechAtlasException extends RuntimeException {
    private final HttpStatus status;

    public TechAtlasException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public TechAtlasException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
