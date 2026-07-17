package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class DuplicateDocumentException extends TechAtlasException {
    public DuplicateDocumentException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
