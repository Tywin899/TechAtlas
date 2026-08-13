package com.techatlas.exception;

import org.springframework.http.HttpStatus;

public class GitHubRateLimitExceededException extends TechAtlasException {
    public GitHubRateLimitExceededException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    public GitHubRateLimitExceededException(String message, Throwable cause) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, cause);
    }
}
