package com.example.backend.orders.model;

public enum ApplicationStatus {
    PENDING("Pending"),
    SELECTED("Selected"),
    REJECTED("Rejected");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
