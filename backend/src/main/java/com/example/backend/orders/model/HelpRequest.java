package com.example.backend.orders.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HelpRequest {
    private final long id;
    private final long requesterUserId;
    private final String title;
    private final String description;
    private final String category;
    private final String location;
    private final String reward;
    private final LocalDateTime serviceTime;
    private final LocalDateTime createdAt;
    private HelpRequestStatus status;
    private Long currentOrderId;
    private final List<RequestApplication> applications;

    public HelpRequest(
            long id,
            long requesterUserId,
            String title,
            String description,
            String category,
            String location,
            String reward,
            LocalDateTime serviceTime,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.requesterUserId = requesterUserId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.reward = reward;
        this.serviceTime = serviceTime;
        this.createdAt = createdAt;
        this.status = HelpRequestStatus.OPEN;
        this.applications = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public long getRequesterUserId() {
        return requesterUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public String getReward() {
        return reward;
    }

    public LocalDateTime getServiceTime() {
        return serviceTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public HelpRequestStatus getStatus() {
        return status;
    }

    public Long getCurrentOrderId() {
        return currentOrderId;
    }

    public List<RequestApplication> getApplications() {
        return Collections.unmodifiableList(applications);
    }

    public void addApplication(RequestApplication application) {
        applications.add(application);
    }

    public void markOrderCreated(long orderId) {
        status = HelpRequestStatus.ORDER_CREATED;
        currentOrderId = orderId;
    }

    public void markCompleted() {
        status = HelpRequestStatus.COMPLETED;
    }

    public void restoreState(HelpRequestStatus restoredStatus, Long restoredOrderId) {
        this.status = restoredStatus;
        this.currentOrderId = restoredOrderId;
    }
}
