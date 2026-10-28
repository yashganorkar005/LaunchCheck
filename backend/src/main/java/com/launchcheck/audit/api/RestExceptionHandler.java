package com.launchcheck.audit.api;

import com.launchcheck.audit.safety.UnsafeTargetException;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(UnsafeTargetException.class)
    public ResponseEntity<ApiError> onUnsafeTarget(UnsafeTargetException ex) {
        String code = ex.code();
        String message = switch (code) {
            case "UNSAFE_URL" -> "Target URL is not a supported HTTP/HTTPS URL.";
            case "UNSUPPORTED_PORT" -> "Only ports 80 (HTTP) and 443 (HTTPS) are supported.";
            case "UNSAFE_DESTINATION" -> "Target resolves to a prohibited IP address.";
            case "DNS_FAILURE" -> "DNS resolution failed for the target.";
            default -> "The target URL is not allowed.";
        };
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(code, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> onIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiError> onResponseStatus(org.springframework.web.server.ResponseStatusException ex) {
        String code = ex.getStatusCode().value() == 429 ? "TOO_MANY_REQUESTS" : "ERROR";
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiError(code, ex.getReason() != null ? ex.getReason() : ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onValidation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_ERROR", msg));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> onMissingHeader(org.springframework.web.bind.MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("BAD_REQUEST", "Missing required header: " + ex.getHeaderName()));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> onMissingParam(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("BAD_REQUEST", "Missing required parameter: " + ex.getParameterName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onOther(Exception ex) {
        // Never leak details; log on the server is handled by Spring.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred while running the audit."));
    }
}
