package com.example.backend.service;

import com.example.backend.entity.Order;
import com.example.backend.entity.Review;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    // 有效评分范围
    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;

    /**
     * 创建评价（订单完成后，双方各可评价一次）
     * @param orderId 订单ID
     * @param reviewerId 评价者ID
     * @param score 评分（1-5）
     * @param content 评价内容（可选）
     * @return 创建的评价
     */
    @Transactional
    public Review createReview(Long orderId, Long reviewerId, Integer score, String content) {
        // 验证评分范围
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new RuntimeException("评分必须在 " + MIN_SCORE + " 到 " + MAX_SCORE + " 之间");
        }

        // 检查订单是否存在
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }

        Order order = orderOpt.get();

        // 检查订单状态是否为已完成
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("只有已完成的订单才能进行评价");
        }

        // 检查评价者是否为订单相关方（发布者或接单者）
        if (!order.getPublisherId().equals(reviewerId) && !order.getAcceptorId().equals(reviewerId)) {
            throw new RuntimeException("只有订单参与方才能进行评价");
        }

        // 检查是否已经评价过
        if (reviewRepository.hasReviewed(orderId, reviewerId)) {
            throw new RuntimeException("您已经评价过此订单");
        }

        // 确定被评价者（评价对方）
        Long reviewedId = order.getPublisherId().equals(reviewerId) 
            ? order.getAcceptorId() 
            : order.getPublisherId();

        // 创建评价
        Review review = new Review(orderId, reviewerId, reviewedId, score, content);
        Review savedReview = reviewRepository.save(review);

        // 更新被评价用户的平均评分
        userService.updateUserScore(reviewedId, score);

        // 如果双方都已完成评价，可以更新订单的commentId（可选）
        Long reviewCount = reviewRepository.countReviewsByOrderId(orderId);
        if (reviewCount == 2) {
            // 双方都评价完了，可以做一些额外处理，比如更新订单状态或记录
            // 这里选择将第一个评价的ID记录到订单中
            if (order.getCommentId() == null) {
                order.setCommentId(savedReview.getId());
                orderRepository.save(order);
            }
        }

        return savedReview;
    }

    /**
     * 根据ID获取评价
     */
    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    /**
     * 根据订单ID获取评价（可能返回两个评价）
     */
    public List<Review> getReviewsByOrderId(Long orderId) {
        return reviewRepository.findAllByOrderId(orderId);
    }

    /**
     * 获取用户收到的评价（作为被评价者）
     */
    public Page<Review> getReviewsReceived(Long userId, Pageable pageable) {
        return reviewRepository.findByReviewedId(userId, pageable);
    }

    /**
     * 获取用户发出的评价（作为评价者）
     */
    public Page<Review> getReviewsGiven(Long userId, Pageable pageable) {
        return reviewRepository.findByReviewerId(userId, pageable);
    }

    /**
     * 获取用户收到的评价统计信息
     */
    public ReviewStatistics getUserReviewStatistics(Long userId) {
        ReviewStatistics stats = new ReviewStatistics();
        stats.setUserId(userId);
        
        Double avgScore = reviewRepository.getAverageScoreForUser(userId);
        Long totalCount = reviewRepository.getScoreCountForUser(userId);
        
        stats.setAverageScore(avgScore != null ? Math.round(avgScore * 100) / 100.0 : 0.0);
        stats.setTotalCount(totalCount != null ? totalCount : 0L);
        
        // 获取各分数段数量
        stats.setScore1Count(countByScoreAndReviewed(userId, 1));
        stats.setScore2Count(countByScoreAndReviewed(userId, 2));
        stats.setScore3Count(countByScoreAndReviewed(userId, 3));
        stats.setScore4Count(countByScoreAndReviewed(userId, 4));
        stats.setScore5Count(countByScoreAndReviewed(userId, 5));
        
        return stats;
    }

    /**
     * 多条件查询评价
     */
    public Page<Review> searchReviews(Long reviewerId, Long reviewedId, Long orderId, 
                                       Integer minScore, Integer maxScore, String keyword,
                                       Pageable pageable) {
        Specification<Review> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (reviewerId != null) {
                predicates.add(cb.equal(root.get("reviewerId"), reviewerId));
            }
            if (reviewedId != null) {
                predicates.add(cb.equal(root.get("reviewedId"), reviewedId));
            }
            if (orderId != null) {
                predicates.add(cb.equal(root.get("orderId"), orderId));
            }
            if (minScore != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("score"), minScore));
            }
            if (maxScore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("score"), maxScore));
            }
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(cb.like(root.get("content"), "%" + keyword + "%"));
            }
            
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return reviewRepository.findAll(spec, pageable);
    }

    /**
     * 获取用户对特定订单的评价
     */
    public Optional<Review> getUserReviewForOrder(Long orderId, Long userId) {
        return reviewRepository.findByOrderIdAndReviewerId(orderId, userId);
    }

    /**
     * 检查订单是否已被双方评价
     */
    public boolean isOrderFullyReviewed(Long orderId) {
        Long count = reviewRepository.countReviewsByOrderId(orderId);
        return count != null && count == 2;
    }

    /**
     * 删除评价（管理员操作）
     */
    @Transactional
    public void deleteReview(Long reviewId, Long adminId) {
        // 检查管理员权限
        if (!userService.isAdmin(adminId)) {
            throw new RuntimeException("只有管理员可以删除评价");
        }
        
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("评价不存在");
        }
        
        Review review = reviewOpt.get();
        
        // 删除评价后，需要重新计算被评价者的平均分
        reviewRepository.delete(review);
        
        // 重新计算用户评分
        recalculateUserAverageScore(review.getReviewedId());
    }

    /**
     * 重新计算用户的平均评分
     */
    private void recalculateUserAverageScore(Long userId) {
        Double newAvg = reviewRepository.getAverageScoreForUser(userId);
        Long newCount = reviewRepository.getScoreCountForUser(userId);
        
        if (newAvg != null && newCount != null) {
            userRepository.updateUserScore(userId, newAvg, newCount);
        } else {
            // 如果没有评价了，重置评分
            userRepository.updateUserScore(userId, 0.0, 0L);
        }
    }

    /**
     * 获取指定分数段的评价数量
     */
    private long countByScoreAndReviewed(Long reviewedId, int score) {
        Page<Review> page = reviewRepository.findByReviewedIdAndScore(reviewedId, score, Pageable.unpaged());
        return page.getTotalElements();
    }

    // ========== 内部类 ==========

    /**
     * 评价统计信息
     */
    public static class ReviewStatistics {
        private Long userId;
        private Double averageScore;
        private Long totalCount;
        private Long score1Count;
        private Long score2Count;
        private Long score3Count;
        private Long score4Count;
        private Long score5Count;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Double getAverageScore() { return averageScore; }
        public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }
        
        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        
        public Long getScore1Count() { return score1Count; }
        public void setScore1Count(Long score1Count) { this.score1Count = score1Count; }
        
        public Long getScore2Count() { return score2Count; }
        public void setScore2Count(Long score2Count) { this.score2Count = score2Count; }
        
        public Long getScore3Count() { return score3Count; }
        public void setScore3Count(Long score3Count) { this.score3Count = score3Count; }
        
        public Long getScore4Count() { return score4Count; }
        public void setScore4Count(Long score4Count) { this.score4Count = score4Count; }
        
        public Long getScore5Count() { return score5Count; }
        public void setScore5Count(Long score5Count) { this.score5Count = score5Count; }
        
        // 高分率（4分及以上）
        public double getHighScoreRate() {
            long highScores = (score4Count != null ? score4Count : 0) + (score5Count != null ? score5Count : 0);
            long total = totalCount != null ? totalCount : 0;
            return total == 0 ? 0 : Math.round((double) highScores / total * 10000) / 100.0;
        }
    }
}