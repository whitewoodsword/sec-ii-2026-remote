package com.example.backend.review.controller;

import com.example.backend.review.dto.ApiResponse;
import com.example.backend.review.dto.CreditInfo;
import com.example.backend.review.dto.SubmitReviewRequest;
import com.example.backend.review.entity.Review;
import com.example.backend.review.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ApiResponse<Review> submit(@RequestBody SubmitReviewRequest req) {
        try {
            Review review = reviewService.submitReview(req);
            return ApiResponse.success(review);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<Review>> getByOrder(@PathVariable Long orderId) {
        return ApiResponse.success(reviewService.getReviewsByOrderId(orderId));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Review>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success(reviewService.getReviewsByUserId(userId));
    }

    @GetMapping("/user/{userId}/credit")
    public ApiResponse<CreditInfo> getCredit(@PathVariable Long userId) {
        return ApiResponse.success(reviewService.getCreditInfo(userId));
    }
}
