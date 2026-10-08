package com.placewise.exception;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public ApiException(HttpStatus status, String message) { super(message); this.status = status; }
    public static ApiException missing(String what) { return new ApiException(HttpStatus.NOT_FOUND, what + " was not found."); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, message); }
    public static ApiException bad(String message) { return new ApiException(HttpStatus.BAD_REQUEST, message); }
}
