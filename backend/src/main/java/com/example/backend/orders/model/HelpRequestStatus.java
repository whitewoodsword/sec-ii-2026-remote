package com.example.backend.orders.model;

public enum HelpRequestStatus {
    OPEN("Open"),
    ORDER_CREATED("Order Created"),
    COMPLETED("Completed"),
    CLOSED("Closed");

    private final String label;

    HelpRequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
