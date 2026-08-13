package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class WikipediaUnavailableException extends TechAtlasException {
    public WikipediaUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public WikipediaUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
