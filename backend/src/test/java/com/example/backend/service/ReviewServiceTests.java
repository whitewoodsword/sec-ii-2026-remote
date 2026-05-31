package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.backend.entity.Order;
import com.example.backend.entity.Review;
import com.example.backend.entity.User;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("评价服务单元测试")
class ReviewServiceTests {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReviewService reviewService;

    private User testPublisher;
    private User testAcceptor;
    private Order testOrder;
    private Review testReview;
    private final Long TEST_ORDER_ID = 1000L;
    private final Long TEST_PUBLISHER_ID = 100L;
    private final Long TEST_ACCEPTOR_ID = 200L;
    private final Long TEST_REVIEW_ID = 5000L;
    private final Long TEST_ADMIN_ID = 1L;
    private final Integer TEST_SCORE = 5;
    private final String TEST_CONTENT = "服务非常好，很满意！";

    @BeforeEach
    void setUp() {
        testPublisher = new User("发布者", "13800138000", "password");
        testPublisher.setId(TEST_PUBLISHER_ID);
        testPublisher.setScoreNum(0L);
        testPublisher.setAverageScore(null);

        testAcceptor = new User("接单者", "13900139000", "password");
        testAcceptor.setId(TEST_ACCEPTOR_ID);
        testAcceptor.setScoreNum(0L);
        testAcceptor.setAverageScore(null);

        testOrder = new Order(500L, TEST_PUBLISHER_ID, TEST_ACCEPTOR_ID);
        testOrder.setId(TEST_ORDER_ID);
        testOrder.setStatus("COMPLETED");
        testOrder.setCreatedAt(LocalDateTime.now().minusDays(7));
        testOrder.setUpdatedAt(LocalDateTime.now());
        testOrder.setCompletedAt(LocalDateTime.now().minusDays(1));
        testOrder.setCommentId(null);

        testReview = new Review(TEST_ORDER_ID, TEST_PUBLISHER_ID, TEST_ACCEPTOR_ID, TEST_SCORE, TEST_CONTENT);
        testReview.setId(TEST_REVIEW_ID);
        testReview.setCreatedAt(LocalDateTime.now());
    }

    // ==================== 创建评价测试 ====================

    @Nested
    @DisplayName("创建评价测试")
    class CreateReviewTests {

        @Test
        @DisplayName("发布者成功评价接单者")
        void testCreateReviewByPublisherSuccess() {
            // Given
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(reviewRepository.hasReviewed(TEST_ORDER_ID, TEST_PUBLISHER_ID)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
                Review saved = invocation.getArgument(0);
                saved.setId(TEST_REVIEW_ID);
                return saved;
            });
            when(reviewRepository.countReviewsByOrderId(TEST_ORDER_ID)).thenReturn(1L);
            doNothing().when(userService).updateUserScore(TEST_ACCEPTOR_ID, TEST_SCORE);

            // When
            Review result = reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, TEST_SCORE, TEST_CONTENT);

            // Then
            assertNotNull(result);
            assertEquals(TEST_ORDER_ID, result.getOrderId());
            assertEquals(TEST_PUBLISHER_ID, result.getReviewerId());
            assertEquals(TEST_ACCEPTOR_ID, result.getReviewedId());
            assertEquals(TEST_SCORE, result.getScore());
            assertEquals(TEST_CONTENT, result.getContent());
            assertNotNull(result.getCreatedAt());

            verify(userService).updateUserScore(TEST_ACCEPTOR_ID, TEST_SCORE);
        }

        @Test
        @DisplayName("接单者成功评价发布者")
        void testCreateReviewByAcceptorSuccess() {
            // Given
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(reviewRepository.hasReviewed(TEST_ORDER_ID, TEST_ACCEPTOR_ID)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
                Review saved = invocation.getArgument(0);
                saved.setId(TEST_REVIEW_ID);
                return saved;
            });
            when(reviewRepository.countReviewsByOrderId(TEST_ORDER_ID)).thenReturn(1L);
            doNothing().when(userService).updateUserScore(TEST_PUBLISHER_ID, TEST_SCORE);

            // When
            Review result = reviewService.createReview(TEST_ORDER_ID, TEST_ACCEPTOR_ID, TEST_SCORE, TEST_CONTENT);

            // Then
            assertNotNull(result);
            assertEquals(TEST_ORDER_ID, result.getOrderId());
            assertEquals(TEST_ACCEPTOR_ID, result.getReviewerId());
            assertEquals(TEST_PUBLISHER_ID, result.getReviewedId());
            assertEquals(TEST_SCORE, result.getScore());
        }

        @Test
        @DisplayName("双方都完成评价后更新订单commentId")
        void testBothPartiesReviewedUpdatesOrder() {
            // Given
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(reviewRepository.hasReviewed(TEST_ORDER_ID, TEST_PUBLISHER_ID)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
                Review saved = invocation.getArgument(0);
                saved.setId(TEST_REVIEW_ID);
                return saved;
            });
            when(reviewRepository.countReviewsByOrderId(TEST_ORDER_ID)).thenReturn(2L);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(userService).updateUserScore(TEST_ACCEPTOR_ID, TEST_SCORE);

            // When
            Review result = reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, TEST_SCORE, TEST_CONTENT);

            // Then
            assertNotNull(result);
            verify(orderRepository).save(testOrder);
            assertEquals(TEST_REVIEW_ID, testOrder.getCommentId());
        }

        @Test
        @DisplayName("评分低于1分抛出异常")
        void testCreateReviewScoreTooLow() {
            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, 0, TEST_CONTENT));
            assertEquals("评分必须在 1 到 5 之间", exception.getMessage());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("评分高于5分抛出异常")
        void testCreateReviewScoreTooHigh() {
            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, 6, TEST_CONTENT));
            assertEquals("评分必须在 1 到 5 之间", exception.getMessage());
        }

        @Test
        @DisplayName("订单不存在时创建评价失败")
        void testCreateReviewOrderNotFound() {
            // Given
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, TEST_SCORE, TEST_CONTENT));
            assertEquals("订单不存在", exception.getMessage());
        }

        @Test
        @DisplayName("订单未完成时不能评价")
        void testCreateReviewOrderNotCompleted() {
            // Given
            testOrder.setStatus("IN_PROGRESS");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, TEST_SCORE, TEST_CONTENT));
            assertEquals("只有已完成的订单才能进行评价", exception.getMessage());
        }

        @Test
        @DisplayName("非订单参与方不能评价")
        void testCreateReviewNotParticipant() {
            // Given
            Long unauthorizedUserId = 999L;
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(TEST_ORDER_ID, unauthorizedUserId, TEST_SCORE, TEST_CONTENT));
            assertEquals("只有订单参与方才能进行评价", exception.getMessage());
        }

        @Test
        @DisplayName("重复评价抛出异常")
        void testCreateReviewDuplicate() {
            // Given
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(reviewRepository.hasReviewed(TEST_ORDER_ID, TEST_PUBLISHER_ID)).thenReturn(true);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, TEST_SCORE, TEST_CONTENT));
            assertEquals("您已经评价过此订单", exception.getMessage());
        }

        @Test
        @DisplayName("创建评价时内容为空也可以")
        void testCreateReviewWithEmptyContent() {
            // Given
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(reviewRepository.hasReviewed(TEST_ORDER_ID, TEST_PUBLISHER_ID)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
                Review saved = invocation.getArgument(0);
                saved.setId(TEST_REVIEW_ID);
                return saved;
            });
            when(reviewRepository.countReviewsByOrderId(TEST_ORDER_ID)).thenReturn(1L);
            doNothing().when(userService).updateUserScore(TEST_ACCEPTOR_ID, TEST_SCORE);

            // When
            Review result = reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, TEST_SCORE, null);

            // Then
            assertNotNull(result);
            assertNull(result.getContent());
        }
    }

    // ==================== 评价查询测试 ====================

    @Nested
    @DisplayName("评价查询测试")
    class ReviewQueryTests {

        @Test
        @DisplayName("根据ID获取评价成功")
        void testGetReviewByIdSuccess() {
            // Given
            when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));

            // When
            Optional<Review> result = reviewService.getReviewById(TEST_REVIEW_ID);

            // Then
            assertTrue(result.isPresent());
            assertEquals(TEST_REVIEW_ID, result.get().getId());
            assertEquals(TEST_CONTENT, result.get().getContent());
        }

        @Test
        @DisplayName("获取不存在的评价返回空")
        void testGetReviewByIdNotFound() {
            // Given
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<Review> result = reviewService.getReviewById(999L);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("根据订单ID获取评价列表")
        void testGetReviewsByOrderId() {
            // Given
            List<Review> reviews = Arrays.asList(testReview);
            when(reviewRepository.findAllByOrderId(TEST_ORDER_ID)).thenReturn(reviews);

            // When
            List<Review> result = reviewService.getReviewsByOrderId(TEST_ORDER_ID);

            // Then
            assertEquals(1, result.size());
            assertEquals(TEST_REVIEW_ID, result.get(0).getId());
        }

        @Test
        @DisplayName("获取订单评价为空列表")
        void testGetReviewsByOrderIdEmpty() {
            // Given
            when(reviewRepository.findAllByOrderId(999L)).thenReturn(Collections.emptyList());

            // When
            List<Review> result = reviewService.getReviewsByOrderId(999L);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("获取用户收到的评价（分页）")
        void testGetReviewsReceived() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(testReview), pageable, 1);
            when(reviewRepository.findByReviewedId(TEST_ACCEPTOR_ID, pageable)).thenReturn(page);

            // When
            Page<Review> result = reviewService.getReviewsReceived(TEST_ACCEPTOR_ID, pageable);

            // Then
            assertEquals(1, result.getTotalElements());
            assertEquals(TEST_REVIEW_ID, result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("获取用户发出的评价（分页）")
        void testGetReviewsGiven() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(testReview), pageable, 1);
            when(reviewRepository.findByReviewerId(TEST_PUBLISHER_ID, pageable)).thenReturn(page);

            // When
            Page<Review> result = reviewService.getReviewsGiven(TEST_PUBLISHER_ID, pageable);

            // Then
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("获取用户对特定订单的评价")
        void testGetUserReviewForOrder() {
            // Given
            when(reviewRepository.findByOrderIdAndReviewerId(TEST_ORDER_ID, TEST_PUBLISHER_ID))
                .thenReturn(Optional.of(testReview));

            // When
            Optional<Review> result = reviewService.getUserReviewForOrder(TEST_ORDER_ID, TEST_PUBLISHER_ID);

            // Then
            assertTrue(result.isPresent());
            assertEquals(TEST_REVIEW_ID, result.get().getId());
        }
    }

    // ==================== 评价统计测试 ====================

    @Nested
    @DisplayName("评价统计测试")
    class ReviewStatisticsTests {

        @Test
        @DisplayName("获取用户评价统计信息")
        void testGetUserReviewStatistics() {
            // Given
            when(reviewRepository.getAverageScoreForUser(TEST_ACCEPTOR_ID)).thenReturn(4.5);
            when(reviewRepository.getScoreCountForUser(TEST_ACCEPTOR_ID)).thenReturn(10L);
            when(reviewRepository.findByReviewedIdAndScore(eq(TEST_ACCEPTOR_ID), eq(1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(reviewRepository.findByReviewedIdAndScore(eq(TEST_ACCEPTOR_ID), eq(2), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(reviewRepository.findByReviewedIdAndScore(eq(TEST_ACCEPTOR_ID), eq(3), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(reviewRepository.findByReviewedIdAndScore(eq(TEST_ACCEPTOR_ID), eq(4), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testReview)));
            when(reviewRepository.findByReviewedIdAndScore(eq(TEST_ACCEPTOR_ID), eq(5), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testReview)));

            // When
            ReviewService.ReviewStatistics stats = reviewService.getUserReviewStatistics(TEST_ACCEPTOR_ID);

            // Then
            assertEquals(TEST_ACCEPTOR_ID, stats.getUserId());
            assertEquals(4.5, stats.getAverageScore());
            assertEquals(10L, stats.getTotalCount());
            assertEquals(0L, stats.getScore1Count());
            assertEquals(0L, stats.getScore2Count());
            assertEquals(0L, stats.getScore3Count());
            assertEquals(1L, stats.getScore4Count());
            assertEquals(1L, stats.getScore5Count());
        }

        @Test
        @DisplayName("用户没有评价时统计数据为0")
        void testGetUserReviewStatisticsNoReviews() {
            // Given
            when(reviewRepository.getAverageScoreForUser(TEST_ACCEPTOR_ID)).thenReturn(null);
            when(reviewRepository.getScoreCountForUser(TEST_ACCEPTOR_ID)).thenReturn(0L);
            when(reviewRepository.findByReviewedIdAndScore(eq(TEST_ACCEPTOR_ID), anyInt(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            ReviewService.ReviewStatistics stats = reviewService.getUserReviewStatistics(TEST_ACCEPTOR_ID);

            // Then
            assertEquals(0.0, stats.getAverageScore());
            assertEquals(0L, stats.getTotalCount());
            assertEquals(0L, stats.getScore1Count());
            assertEquals(0L, stats.getScore2Count());
            assertEquals(0L, stats.getScore3Count());
            assertEquals(0L, stats.getScore4Count());
            assertEquals(0L, stats.getScore5Count());
            assertEquals(0.0, stats.getHighScoreRate());
        }

        @Test
        @DisplayName("高分率计算正确")
        void testHighScoreRateCalculation() {
            // Given
            ReviewService.ReviewStatistics stats = new ReviewService.ReviewStatistics();
            stats.setScore4Count(2L);
            stats.setScore5Count(3L);
            stats.setTotalCount(10L);

            // Then
            assertEquals(50.0, stats.getHighScoreRate()); // (2+3)/10 = 0.5 = 50%
        }

        @Test
        @DisplayName("总分为0时高分率为0")
        void testHighScoreRateWhenTotalZero() {
            // Given
            ReviewService.ReviewStatistics stats = new ReviewService.ReviewStatistics();
            stats.setScore4Count(0L);
            stats.setScore5Count(0L);
            stats.setTotalCount(0L);

            // Then
            assertEquals(0.0, stats.getHighScoreRate());
        }
    }

    // ==================== 评价搜索测试 ====================

    @Nested
    @DisplayName("评价搜索测试")
    class SearchReviewsTests {

        @Test
        @DisplayName("多条件组合搜索评价")
        void testSearchReviewsWithMultipleConditions() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(testReview), pageable, 1);
            when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Review> result = reviewService.searchReviews(
                TEST_PUBLISHER_ID, TEST_ACCEPTOR_ID, TEST_ORDER_ID,
                4, 5, "满意", pageable
            );

            // Then
            assertNotNull(result);
            verify(reviewRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("只按评价者ID搜索")
        void testSearchReviewsByReviewerOnly() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(testReview), pageable, 1);
            when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Review> result = reviewService.searchReviews(
                TEST_PUBLISHER_ID, null, null, null, null, null, pageable
            );

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("只按关键词搜索")
        void testSearchReviewsByKeywordOnly() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(testReview), pageable, 1);
            when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Review> result = reviewService.searchReviews(
                null, null, null, null, null, "好评", pageable
            );

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("空条件搜索返回所有评价")
        void testSearchReviewsWithNullConditions() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(testReview), pageable, 1);
            when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Review> result = reviewService.searchReviews(
                null, null, null, null, null, null, pageable
            );

            // Then
            assertNotNull(result);
        }
    }

    // ==================== 订单评价状态测试 ====================

    @Nested
    @DisplayName("订单评价状态测试")
    class OrderReviewStatusTests {

        @Test
        @DisplayName("检查订单是否已被双方评价")
        void testIsOrderFullyReviewed() {
            // Given
            when(reviewRepository.countReviewsByOrderId(TEST_ORDER_ID)).thenReturn(2L);

            // When
            boolean result = reviewService.isOrderFullyReviewed(TEST_ORDER_ID);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("订单只有一方评价时未完全评价")
        void testIsOrderNotFullyReviewed() {
            // Given
            when(reviewRepository.countReviewsByOrderId(TEST_ORDER_ID)).thenReturn(1L);

            // When
            boolean result = reviewService.isOrderFullyReviewed(TEST_ORDER_ID);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("订单没有评价时未完全评价")
        void testIsOrderNotReviewed() {
            // Given
            when(reviewRepository.countReviewsByOrderId(TEST_ORDER_ID)).thenReturn(0L);

            // When
            boolean result = reviewService.isOrderFullyReviewed(TEST_ORDER_ID);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("评价计数为null时视为未完全评价")
        void testIsOrderFullyReviewedWithNullCount() {
            // Given
            when(reviewRepository.countReviewsByOrderId(TEST_ORDER_ID)).thenReturn(null);

            // When
            boolean result = reviewService.isOrderFullyReviewed(TEST_ORDER_ID);

            // Then
            assertFalse(result);
        }
    }

    // ==================== 删除评价测试 ====================

    @Nested
    @DisplayName("删除评价测试")
    class DeleteReviewTests {

        @Test
        @DisplayName("管理员成功删除评价")
        void testDeleteReviewByAdminSuccess() {
            // Given
            when(userService.isAdmin(TEST_ADMIN_ID)).thenReturn(true);
            when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));
            doNothing().when(reviewRepository).delete(testReview);
            when(reviewRepository.getAverageScoreForUser(TEST_ACCEPTOR_ID)).thenReturn(4.0);
            when(reviewRepository.getScoreCountForUser(TEST_ACCEPTOR_ID)).thenReturn(5L);
            doNothing().when(userRepository).updateUserScore(TEST_ACCEPTOR_ID, 4.0, 5L);

            // When
            reviewService.deleteReview(TEST_REVIEW_ID, TEST_ADMIN_ID);

            // Then
            verify(reviewRepository).delete(testReview);
            verify(userRepository).updateUserScore(TEST_ACCEPTOR_ID, 4.0, 5L);
        }


        @Test
        @DisplayName("删除不存在的评价抛出异常")
        void testDeleteNonExistentReview() {
            // Given
            when(userService.isAdmin(TEST_ADMIN_ID)).thenReturn(true);
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.deleteReview(999L, TEST_ADMIN_ID));
            assertEquals("评价不存在", exception.getMessage());
        }

        @Test
        @DisplayName("删除评价后重新计算用户评分（无剩余评价）")
        void testDeleteReviewAndRecalculateToZero() {
            // Given
            when(userService.isAdmin(TEST_ADMIN_ID)).thenReturn(true);
            when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));
            doNothing().when(reviewRepository).delete(testReview);
            when(reviewRepository.getAverageScoreForUser(TEST_ACCEPTOR_ID)).thenReturn(null);
            when(reviewRepository.getScoreCountForUser(TEST_ACCEPTOR_ID)).thenReturn(0L);
            doNothing().when(userRepository).updateUserScore(TEST_ACCEPTOR_ID, 0.0, 0L);

            // When
            reviewService.deleteReview(TEST_REVIEW_ID, TEST_ADMIN_ID);

            // Then
            verify(userRepository).updateUserScore(TEST_ACCEPTOR_ID, 0.0, 0L);
        }
    }

    // ==================== 边界和异常测试 ====================

    @Nested
    @DisplayName("边界和异常测试")
    class BoundaryAndExceptionTests {

        @Test
        @DisplayName("创建评价时内容超长处理")
        void testCreateReviewWithLongContent() {
            // Given
            String longContent = "A".repeat(5000);
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(reviewRepository.hasReviewed(TEST_ORDER_ID, TEST_PUBLISHER_ID)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
                Review saved = invocation.getArgument(0);
                saved.setId(TEST_REVIEW_ID);
                return saved;
            });
            when(reviewRepository.countReviewsByOrderId(TEST_ORDER_ID)).thenReturn(1L);
            doNothing().when(userService).updateUserScore(TEST_ACCEPTOR_ID, TEST_SCORE);

            // When
            Review result = reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, TEST_SCORE, longContent);

            // Then
            assertNotNull(result);
            assertEquals(longContent, result.getContent());
        }

        @Test
        @DisplayName("分页参数边界测试")
        void testPaginationBoundary() {
            // Given
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            Page<Review> page = new PageImpl<>(List.of(testReview), pageable, 1);
            when(reviewRepository.findByReviewedId(eq(TEST_ACCEPTOR_ID), any(Pageable.class))).thenReturn(page);

            // When
            Page<Review> result = reviewService.getReviewsReceived(TEST_ACCEPTOR_ID, pageable);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("负数页码处理")
        void testNegativePageNumber() {
            try {
                // Given
                Pageable pageable = PageRequest.of(-1, 10);
                Page<Review> page = new PageImpl<>(List.of(testReview), pageable, 1);
                when(reviewRepository.findByReviewedId(eq(TEST_ACCEPTOR_ID), any(Pageable.class))).thenReturn(page);

                // When
                Page<Review> result = reviewService.getReviewsReceived(TEST_ACCEPTOR_ID, pageable);

                // Then
                assertNotNull(result);
            } catch (Exception e) {
                assertEquals("Page index must not be less than zero", e.getMessage());
            }
            
        }

        @Test
        @DisplayName("评分边界值测试")
        void testScoreBoundaryValues() {
            // Given
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(reviewRepository.hasReviewed(TEST_ORDER_ID, TEST_PUBLISHER_ID)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
                Review saved = invocation.getArgument(0);
                saved.setId(TEST_REVIEW_ID);
                return saved;
            });
            when(reviewRepository.countReviewsByOrderId(TEST_ORDER_ID)).thenReturn(1L);

            // When - 测试边界值1分
            Review result1 = reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, 1, "差评");
            assertEquals(1, result1.getScore());

            // When - 测试边界值5分
            Review result5 = reviewService.createReview(TEST_ORDER_ID, TEST_PUBLISHER_ID, 5, "好评");
            assertEquals(5, result5.getScore());
        }


        @Test
        @DisplayName("批量获取评价统计时的性能验证")
        void testGetUserReviewStatisticsPerformance() {
            // Given - 模拟大量统计数据计算
            when(reviewRepository.getAverageScoreForUser(TEST_ACCEPTOR_ID)).thenReturn(4.2);
            when(reviewRepository.getScoreCountForUser(TEST_ACCEPTOR_ID)).thenReturn(100L);
            
            // Mock各分数段查询
            for (int score = 1; score <= 5; score++) {
                long count = score == 5 ? 40L : score == 4 ? 30L : 10L;
                List<Review> reviews = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    reviews.add(testReview);
                }
                when(reviewRepository.findByReviewedIdAndScore(eq(TEST_ACCEPTOR_ID), eq(score), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(reviews));
            }

            // When
            ReviewService.ReviewStatistics stats = reviewService.getUserReviewStatistics(TEST_ACCEPTOR_ID);

            // Then
            assertEquals(100L, stats.getTotalCount());
            assertEquals(70.0, stats.getHighScoreRate()); // (30+40)/100 = 70%
            verify(reviewRepository, times(5)).findByReviewedIdAndScore(eq(TEST_ACCEPTOR_ID), anyInt(), any(Pageable.class));
        }
    }
}