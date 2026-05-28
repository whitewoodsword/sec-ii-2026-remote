package com.example.backend.repository;

import com.example.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    /**
     * 根据订单ID查询评价
     */
    Optional<Review> findByOrderId(Long orderId);

    /**
     * 根据订单ID和评价者ID查询是否已评价
     */
    Optional<Review> findByOrderIdAndReviewerId(Long orderId, Long reviewerId);

    /**
     * 查询用户收到的评价（作为被评价者）
     */
    Page<Review> findByReviewedId(Long reviewedId, Pageable pageable);

    /**
     * 查询用户发出的评价（作为评价者）
     */
    Page<Review> findByReviewerId(Long reviewerId, Pageable pageable);

    /**
     * 查询用户收到的评价中指定分数的
     */
    Page<Review> findByReviewedIdAndScore(Long reviewedId, Integer score, Pageable pageable);

    /**
     * 查询用户发出的评价中指定分数的
     */
    Page<Review> findByReviewerIdAndScore(Long reviewerId, Integer score, Pageable pageable);

    /**
     * 获取用户的平均评分
     */
    @Query("SELECT AVG(r.score) FROM Review r WHERE r.reviewedId = :userId")
    Double getAverageScoreForUser(@Param("userId") Long userId);

    /**
     * 获取用户的评分数量
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewedId = :userId")
    Long getScoreCountForUser(@Param("userId") Long userId);

    /**
     * 检查用户是否已对某个订单进行评价
     */
    @Query("SELECT COUNT(r) > 0 FROM Review r WHERE r.orderId = :orderId AND r.reviewerId = :reviewerId")
    boolean hasReviewed(@Param("orderId") Long orderId, @Param("reviewerId") Long reviewerId);

    /**
     * 检查订单双方是否都已评价
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.orderId = :orderId")
    Long countReviewsByOrderId(@Param("orderId") Long orderId);

    /**
     * 获取订单双方的评价
     */
    @Query("SELECT r FROM Review r WHERE r.orderId = :orderId")
    java.util.List<Review> findAllByOrderId(@Param("orderId") Long orderId);

    /**
     * 删除订单相关的所有评价
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Review r WHERE r.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);
}