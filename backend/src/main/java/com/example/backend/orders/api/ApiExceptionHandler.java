package com.example.backend.orders.api;

import com.example.backend.orders.service.OrderModuleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OrderModuleException.class)
    public ResponseEntity<ApiError> handleOrderModuleException(OrderModuleException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(exception.getMessage()));
    }

    public record ApiError(String message) {
    }
}
