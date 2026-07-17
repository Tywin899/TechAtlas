package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class DocumentNotFoundException extends TechAtlasException {
    public DocumentNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
