package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewed_id", nullable = false)
    private Long reviewedId;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "content")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Review() {}

    public Review(Long orderId, Long reviewerId, Long reviewedId, Integer score, String content) {
        this.orderId = orderId;
        this.reviewerId = reviewerId;
        this.reviewedId = reviewedId;
        this.score = score;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

    public Long getReviewedId() { return reviewedId; }
    public void setReviewedId(Long reviewedId) { this.reviewedId = reviewedId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}