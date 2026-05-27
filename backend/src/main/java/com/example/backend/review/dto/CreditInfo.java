package com.example.backend.review.dto;

public class CreditInfo {

    private Long userId;
    private Double avgRating;
    private Integer reviewCount;
    private Double creditScore;
    private String creditLevel;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public Double getCreditScore() { return creditScore; }
    public void setCreditScore(Double creditScore) { this.creditScore = creditScore; }

    public String getCreditLevel() { return creditLevel; }
    public void setCreditLevel(String creditLevel) { this.creditLevel = creditLevel; }
}
