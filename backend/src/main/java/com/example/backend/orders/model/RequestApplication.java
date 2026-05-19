package com.example.backend.orders.model;

import java.time.LocalDateTime;

public class RequestApplication {
    private final long id;
    private final long requestId;
    private final long applicantUserId;
    private final String message;
    private final LocalDateTime createdAt;
    private ApplicationStatus status;

    public RequestApplication(long id, long requestId, long applicantUserId, String message, LocalDateTime createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.applicantUserId = applicantUserId;
        this.message = message;
        this.createdAt = createdAt;
        this.status = ApplicationStatus.PENDING;
    }

    public long getId() {
        return id;
    }

    public long getRequestId() {
        return requestId;
    }

    public long getApplicantUserId() {
        return applicantUserId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void select() {
        status = ApplicationStatus.SELECTED;
    }

    public void reject() {
        status = ApplicationStatus.REJECTED;
    }

    public void restoreStatus(ApplicationStatus restoredStatus) {
        this.status = restoredStatus;
    }
}
