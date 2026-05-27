package com.example.backend.orders.model;

public enum OrderAction {
    START,
    SUBMIT_COMPLETION,
    CONFIRM_COMPLETION,
    REJECT_COMPLETION;

    public static OrderAction fromText(String value) {
        try {
            return OrderAction.valueOf(value.trim().toUpperCase());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unsupported order action: " + value);
        }
    }
}
