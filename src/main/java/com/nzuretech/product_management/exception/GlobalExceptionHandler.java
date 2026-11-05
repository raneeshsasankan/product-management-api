package com.nzuretech.product_management.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handler for REST API error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles product not found exceptions.
     *
     * @param ex the caught exception
     * @return a 404 Not Found response
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(
            ProductNotFoundException ex) {

        Map<String, Object> response = createErrorResponse(
                HttpStatus.NOT_FOUND, ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles request validation failures.
     *
     * @param ex the validation exception
     * @return a 400 Bad Request response containing field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> validationErrors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.putIfAbsent(
                        error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST, null);

        response.put("validationErrors", validationErrors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles unexpected application exceptions.
     *
     * @param ex the unexpected exception
     * @return a 500 Internal Server Error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception ex) {

        logger.error("Unexpected error occurred while processing request", ex);
        Map<String, Object> response = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Creates a standardized error response.
     *
     * @param status the HTTP status
     * @param message the error message
     * @return the standardized error response
     */
    private Map<String, Object> createErrorResponse(
            HttpStatus status, String message) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());

        if (message != null) {
            response.put("message", message);
        }

        return response;
    }
}
