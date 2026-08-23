package com.transport.erp.exception;

import com.transport.erp.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void badCredentialsReturnUnauthorized() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBadCredentialsException(new BadCredentialsException("bad"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(Boolean.TRUE.equals(response.getBody().isSuccess()));
        assertEquals("Authentication failure", response.getBody().getMessage());
    }

    @Test
    void illegalArgumentReturnBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("missing field"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation or argument error", response.getBody().getMessage());
        assertEquals("missing field", response.getBody().getErrors().get(0));
    }
}
