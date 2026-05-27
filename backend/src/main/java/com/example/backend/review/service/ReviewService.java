package com.example.backend.review.service;

import com.example.backend.review.dto.CreditInfo;
import com.example.backend.review.dto.SubmitReviewRequest;
import com.example.backend.review.entity.Review;
import com.example.backend.review.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;

    public ReviewService(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    public Review submitReview(SubmitReviewRequest req) {
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new IllegalArgumentException("评分必须在1-5之间");
        }
        String role = req.getReviewerRole();
        if (role == null || (!role.equals("DEMANDER") && !role.equals("SERVER"))) {
            throw new IllegalArgumentException("评价人角色必须为 DEMANDER 或 SERVER");
        }
        if (reviewMapper.existsByOrderIdAndReviewerId(req.getOrderId(), req.getReviewerId())) {
            throw new IllegalStateException("您已对该订单进行过评价，不能重复评价");
        }

        Review review = new Review();
        review.setOrderId(req.getOrderId());
        review.setReviewerId(req.getReviewerId());
        review.setRevieweeId(req.getRevieweeId());
        review.setRating(req.getRating());
        review.setComment(req.getComment() != null ? req.getComment() : "");
        review.setReviewerRole(role);

        reviewMapper.insert(review);
        return reviewMapper.selectById(review.getId());
    }

    public List<Review> getReviewsByOrderId(Long orderId) {
        return reviewMapper.selectByOrderId(orderId);
    }

    public List<Review> getReviewsByUserId(Long userId) {
        return reviewMapper.selectByRevieweeId(userId);
    }

    public CreditInfo getCreditInfo(Long userId) {
        Double avgRating = reviewMapper.selectAvgRatingByUserId(userId);
        Integer count = reviewMapper.countByUserId(userId);

        CreditInfo info = new CreditInfo();
        info.setUserId(userId);
        info.setAvgRating(avgRating);
        info.setReviewCount(count);
        info.setCreditScore(avgRating * 20.0);
        info.setCreditLevel(calcLevel(info.getCreditScore(), count));
        return info;
    }

    private String calcLevel(double score, int count) {
        if (count == 0) return "暂无";
        if (score >= 90) return "优秀";
        if (score >= 70) return "良好";
        if (score >= 50) return "一般";
        return "较差";
    }
}
