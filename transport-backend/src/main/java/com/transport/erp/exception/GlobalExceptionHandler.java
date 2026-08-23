package com.transport.erp.exception;

import com.transport.erp.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiResponse<Void> response = ApiResponse.error(
                Collections.singletonList(ex.getMessage()),
                "Validation or argument error"
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        ApiResponse<Void> response = ApiResponse.error(
                Collections.singletonList("Invalid username or password"),
                "Authentication failure"
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ApiResponse<Void> response = ApiResponse.error(
                Collections.singletonList(ex.getMessage()),
                "Access denied"
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> String.format("Field '%s': %s", err.getField(), err.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiResponse<Void> response = ApiResponse.error(errors, "Input validation failed");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String message = "Could not save data due to a database constraint";
        if (detail != null) {
            String lower = detail.toLowerCase();
            if (lower.contains("app_roles")) {
                message = "A role with this code already exists for the tenant";
            } else if (lower.contains("app_settings")) {
                message = "A setting key already exists for this company";
            } else if (lower.contains("financial_years")) {
                message = "A financial year with this code already exists";
            } else if (lower.contains("companies") && lower.contains("unique")) {
                message = "A company with this code already exists";
            } else if (lower.contains("username")) {
                message = "This username is already taken";
            }
        }
        ApiResponse<Void> response = ApiResponse.error(
                Collections.singletonList(detail != null ? detail : message),
                message
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        ApiResponse<Void> response = ApiResponse.error(
                Collections.singletonList(ex.getMessage()),
                "An unexpected server error occurred"
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
