package com.placewise.exception;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public record ErrorBody(String message, Map<String, String> errors, Instant timestamp) {}
    private ResponseEntity<ErrorBody> error(HttpStatus status, String message, Map<String,String> fields) {
        return ResponseEntity.status(status).body(new ErrorBody(message, fields, Instant.now()));
    }
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> domain(ApiException e) { return error(e.status, e.getMessage(), Map.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException e) {
        Map<String,String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Please check the highlighted fields.", fields);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorBody> duplicate(DataIntegrityViolationException e) {
        return error(HttpStatus.CONFLICT, "This email, roll number, skill, or application already exists. Please refresh and try again.", Map.of());
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorBody> malformed(Exception e) { return error(HttpStatus.BAD_REQUEST, "The request contains an invalid value.", Map.of()); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> forbidden(AccessDeniedException e) { return error(HttpStatus.FORBIDDEN, "You do not have access to this action.", Map.of()); }
}
