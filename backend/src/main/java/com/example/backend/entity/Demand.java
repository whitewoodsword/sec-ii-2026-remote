package com.example.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demands")
public class Demand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "reward")
    private Double reward;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "picture_urls")
    private String pictureUrls;

    public Demand() {}

    public Demand(String title, String description, String category, Long publisherId, 
                  String location, LocalDateTime deadline, Double reward, String pictureUrls) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.publisherId = publisherId;
        this.status = "PENDING";
        this.location = location;
        this.deadline = deadline;
        this.reward = reward;
        this.pictureUrls = pictureUrls;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Double getReward() { return reward; }
    public void setReward(Double reward) { this.reward = reward; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getPictureUrls() { return pictureUrls; }
    public void setPictureUrls(String pictureUrls) { this.pictureUrls = pictureUrls; }
}