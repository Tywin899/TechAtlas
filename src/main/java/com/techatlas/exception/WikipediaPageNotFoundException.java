package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class WikipediaPageNotFoundException extends TechAtlasException {
    public WikipediaPageNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
