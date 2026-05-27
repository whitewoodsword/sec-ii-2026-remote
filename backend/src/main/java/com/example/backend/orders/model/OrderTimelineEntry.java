package com.example.backend.orders.model;

import java.time.LocalDateTime;

public record OrderTimelineEntry(LocalDateTime happenedAt, String actorName, String description) {
}
