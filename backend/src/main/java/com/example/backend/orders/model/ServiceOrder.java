package com.example.backend.orders.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceOrder {
    private final long id;
    private final long requestId;
    private final long requesterUserId;
    private final long providerUserId;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private String latestRequesterNote;
    private final List<OrderTimelineEntry> timelineEntries;

    public ServiceOrder(long id, long requestId, long requesterUserId, long providerUserId, LocalDateTime createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.requesterUserId = requesterUserId;
        this.providerUserId = providerUserId;
        this.status = OrderStatus.PENDING_ACCEPTANCE;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.timelineEntries = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public long getRequestId() {
        return requestId;
    }

    public long getRequesterUserId() {
        return requesterUserId;
    }

    public long getProviderUserId() {
        return providerUserId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getLatestRequesterNote() {
        return latestRequesterNote;
    }

    public List<OrderTimelineEntry> getTimelineEntries() {
        return Collections.unmodifiableList(timelineEntries);
    }

    public void updateStatus(OrderStatus status, LocalDateTime happenedAt) {
        this.status = status;
        this.updatedAt = happenedAt;
        if (status == OrderStatus.COMPLETED) {
            this.completedAt = happenedAt;
        } else {
            this.completedAt = null;
        }
    }

    public void setLatestRequesterNote(String latestRequesterNote) {
        this.latestRequesterNote = latestRequesterNote;
    }

    public void addTimelineEntry(OrderTimelineEntry entry) {
        timelineEntries.add(entry);
    }

    public void restoreState(
            OrderStatus restoredStatus,
            LocalDateTime restoredUpdatedAt,
            LocalDateTime restoredCompletedAt,
            String restoredRequesterNote
    ) {
        this.status = restoredStatus;
        this.updatedAt = restoredUpdatedAt;
        this.completedAt = restoredCompletedAt;
        this.latestRequesterNote = restoredRequesterNote;
    }
}
