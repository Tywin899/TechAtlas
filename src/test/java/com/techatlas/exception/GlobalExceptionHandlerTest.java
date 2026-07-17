package com.techatlas.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void handleTechAtlasExceptionReturnsCorrectResponse() {
        TechAtlasException exception = new TechAtlasException(HttpStatus.BAD_REQUEST, "Invalid input query");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTechAtlasException(exception, request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("Bad Request", body.getError());
        assertEquals("Invalid input query", body.getMessage());
        assertEquals("/api/v1/test", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void handleIllegalArgumentExceptionReturnsBadRequest() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument format");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequestException(exception, request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("Bad Request", body.getError());
        assertEquals("Invalid argument format", body.getMessage());
    }

    @Test
    void handleGenericExceptionReturnsInternalServerError() {
        RuntimeException exception = new RuntimeException("DB Connection failure");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, request);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getStatus());
        assertEquals("Internal Server Error", body.getError());
        assertEquals("An unexpected internal error occurred.", body.getMessage());
    }
}
