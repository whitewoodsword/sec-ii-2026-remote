package com.example.backend.orders.model;

public enum OrderStatus {
    PENDING_ACCEPTANCE("Pending Start"),
    IN_PROGRESS("In Progress"),
    PENDING_CONFIRMATION("Pending Confirmation"),
    COMPLETED("Completed");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
