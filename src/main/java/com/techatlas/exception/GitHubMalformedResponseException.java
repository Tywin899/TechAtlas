package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class GitHubMalformedResponseException extends TechAtlasException {
    public GitHubMalformedResponseException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public GitHubMalformedResponseException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
