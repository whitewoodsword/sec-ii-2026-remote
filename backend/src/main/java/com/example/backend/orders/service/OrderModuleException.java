package com.example.backend.orders.service;

import org.springframework.http.HttpStatus;

public class OrderModuleException extends RuntimeException {
    private final HttpStatus status;

    public OrderModuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
