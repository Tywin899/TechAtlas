package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class WikipediaMalformedResponseException extends TechAtlasException {
    public WikipediaMalformedResponseException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public WikipediaMalformedResponseException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
