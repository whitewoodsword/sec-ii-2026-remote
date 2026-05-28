package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.entity.Review;
import com.example.backend.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 创建评价
     * POST /reviews/create?orderId={orderId}&userId={reviewerId}&score={score}&content={content}
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Review>> createReview(
            @RequestParam Long orderId,
            @RequestParam Long userId,
            @RequestParam Integer score,
            @RequestParam(required = false) String content) {
        
        try {
            Review review = reviewService.createReview(orderId, userId, score, content);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(review));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 获取评价详情
     * GET /reviews/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Review>> getReviewById(@PathVariable Long id) {
        Optional<Review> reviewOpt = reviewService.getReviewById(id);
        if (reviewOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(reviewOpt.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "评价不存在"));
        }
    }

    /**
     * 根据订单ID获取评价（可能返回两个评价）
     * GET /reviews/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<Review>>> getReviewsByOrderId(@PathVariable Long orderId) {
        List<Review> reviews = reviewService.getReviewsByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * 获取用户收到的评价（作为被评价者）
     * GET /reviews/received/{userId}?page=0&size=10&score=可选
     */
    @GetMapping("/received/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviewsReceived(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer score,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Review> reviewPage;
        if (score != null) {
            // 需要添加按分数筛选的方法到Service
            reviewPage = reviewService.searchReviews(null, userId, null, score, score, null, pageable);
        } else {
            reviewPage = reviewService.getReviewsReceived(userId, pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", reviewPage.getContent());
        response.put("totalElements", reviewPage.getTotalElements());
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("currentPage", reviewPage.getNumber());
        response.put("pageSize", reviewPage.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取用户发出的评价（作为评价者）
     * GET /reviews/given/{userId}?page=0&size=10&score=可选
     */
    @GetMapping("/given/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviewsGiven(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer score,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Review> reviewPage;
        if (score != null) {
            reviewPage = reviewService.searchReviews(userId, null, null, score, score, null, pageable);
        } else {
            reviewPage = reviewService.getReviewsGiven(userId, pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", reviewPage.getContent());
        response.put("totalElements", reviewPage.getTotalElements());
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("currentPage", reviewPage.getNumber());
        response.put("pageSize", reviewPage.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取用户评价统计信息
     * GET /reviews/statistics/{userId}
     */
    @GetMapping("/statistics/{userId}")
    public ResponseEntity<ApiResponse<ReviewService.ReviewStatistics>> getReviewStatistics(@PathVariable Long userId) {
        ReviewService.ReviewStatistics stats = reviewService.getUserReviewStatistics(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 多条件搜索评价
     * GET /reviews/search?reviewerId=&reviewedId=&orderId=&minScore=&maxScore=&keyword=&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchReviews(
            @RequestParam(required = false) Long reviewerId,
            @RequestParam(required = false) Long reviewedId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer maxScore,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Review> reviewPage = reviewService.searchReviews(reviewerId, reviewedId, orderId, 
                                                               minScore, maxScore, keyword, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", reviewPage.getContent());
        response.put("totalElements", reviewPage.getTotalElements());
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("currentPage", reviewPage.getNumber());
        response.put("pageSize", reviewPage.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取用户对特定订单的评价
     * GET /reviews/user-order?orderId={orderId}&userId={userId}
     */
    @GetMapping("/user-order")
    public ResponseEntity<ApiResponse<Review>> getUserReviewForOrder(
            @RequestParam Long orderId,
            @RequestParam Long userId) {
        
        Optional<Review> reviewOpt = reviewService.getUserReviewForOrder(orderId, userId);
        if (reviewOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(reviewOpt.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "您尚未评价此订单"));
        }
    }

    /**
     * 检查订单是否已被双方评价
     * GET /reviews/order/{orderId}/fully-reviewed
     */
    @GetMapping("/order/{orderId}/fully-reviewed")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> isOrderFullyReviewed(@PathVariable Long orderId) {
        boolean isFullyReviewed = reviewService.isOrderFullyReviewed(orderId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("fullyReviewed", isFullyReviewed);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 删除评价（管理员操作）
     * DELETE /reviews/{id}?adminId={adminId}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        
        try {
            reviewService.deleteReview(id, adminId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("管理员")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(403, e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 获取有效评分范围
     * GET /reviews/score-range
     */
    @GetMapping("/score-range")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getScoreRange() {
        Map<String, Integer> range = new HashMap<>();
        range.put("min", 1);
        range.put("max", 5);
        return ResponseEntity.ok(ApiResponse.success(range));
    }
}