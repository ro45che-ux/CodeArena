package com.platform.backend.config;

import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every failure into a small {@code {"error": "..."}} JSON body with the right HTTP status,
 * and keeps stack traces and internal details out of responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong on the server"));
    }

    /** Re-shape the framework's own error bodies (400, 405, 415, ResponseStatusException, ...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String message;
        if (body instanceof ProblemDetail problem && problem.getDetail() != null) {
            message = problem.getDetail();
        } else {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            message = status != null ? status.getReasonPhrase() : "Request failed";
        }
        if (statusCode.is4xxClientError() && ex instanceof org.springframework.http.converter.HttpMessageNotReadableException) {
            message = "Malformed request body";
        }
        return super.handleExceptionInternal(ex, Map.of("error", message), headers, statusCode, request);
    }
}
