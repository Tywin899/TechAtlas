package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class GitHubUnavailableException extends TechAtlasException {
    public GitHubUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public GitHubUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
