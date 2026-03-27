package com.dairy.dairy_management.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 — validation errors from @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));
        Map<String, Object> response = new HashMap<>();
        response.put("status", 400);
        response.put("errors", fieldErrors);
        return response;
    }

    // 400 — business logic validation errors (invalid input, bad state transitions, etc.)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        return errorBody(400, ex.getMessage());
    }

    // 404 — entity not found
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFoundException(NotFoundException ex) {
        return errorBody(404, ex.getMessage());
    }

    // 409 — conflict: duplicate record, already paid, already active, etc.
    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleConflictException(ConflictException ex) {
        return errorBody(409, ex.getMessage());
    }

    // 400 — catch-all for other business exceptions thrown as RuntimeException
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleRuntimeException(RuntimeException ex) {
        return errorBody(400, ex.getMessage());
    }

    private Map<String, Object> errorBody(int status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", status);
        error.put("error", message);
        return error;
    }
}
