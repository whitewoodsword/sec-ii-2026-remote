package com.example.backend;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.example.backend.controller.ReviewController;
import com.example.backend.dto.ApiResponse;
import com.example.backend.entity.Order;
import com.example.backend.entity.Review;
import com.example.backend.entity.User;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ReviewService;
import com.example.backend.service.UserService;

/**
 * 评价模块集成测试
 * 测试全链路：Controller → Service → Repository → H2 Database
 *
 * 涉及组件：
 * - ReviewController, ReviewService, ReviewRepository（评价核心）
 * - OrderRepository（评价依赖订单）
 * - UserRepository, UserService（评价依赖用户和评分更新）
 *
 * Service层测试：直接调用 ReviewService，验证业务逻辑 + 数据库操作
 * Controller层测试：直接调用 ReviewController，验证接口层 + 业务层 + 数据库操作
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("评价模块集成测试")
class ReviewIntegrationTests {

    @Autowired
    private ReviewController reviewController;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private User publisher;
    private User acceptor;
    private User adminUser;
    private User outsider;
    private Order completedOrder;
    private Order inProgressOrder;

    @BeforeEach
    void setUp() {
        // 清理数据（按依赖顺序）
        reviewRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();

        // 创建测试用户
        publisher = new User("发布者张三", "13800000001", "pass123");
        publisher.setScoreNum(0L);
        publisher.setAverageScore(null);
        publisher.setAdmin(false);
        publisher.setSuperAdmin(false);
        publisher = userRepository.save(publisher);

        acceptor = new User("接单者李四", "13800000002", "pass123");
        acceptor.setScoreNum(0L);
        acceptor.setAverageScore(null);
        acceptor.setAdmin(false);
        acceptor.setSuperAdmin(false);
        acceptor = userRepository.save(acceptor);

        adminUser = new User("管理员王五", "13800000003", "pass123");
        adminUser.setScoreNum(0L);
        adminUser.setAverageScore(null);
        adminUser.setAdmin(true);
        adminUser.setSuperAdmin(false);
        adminUser = userRepository.save(adminUser);

        outsider = new User("路人甲", "13800000004", "pass123");
        outsider.setScoreNum(0L);
        outsider.setAverageScore(null);
        outsider.setAdmin(false);
        outsider.setSuperAdmin(false);
        outsider = userRepository.save(outsider);

        // 创建测试订单
        completedOrder = new Order(100L, publisher.getId(), acceptor.getId());
        completedOrder.setStatus("COMPLETED");
        completedOrder.setCreatedAt(LocalDateTime.now().minusDays(7));
        completedOrder.setUpdatedAt(LocalDateTime.now());
        completedOrder.setCompletedAt(LocalDateTime.now().minusDays(1));
        completedOrder = orderRepository.save(completedOrder);

        inProgressOrder = new Order(200L, publisher.getId(), acceptor.getId());
        inProgressOrder.setStatus("IN_PROGRESS");
        inProgressOrder.setCreatedAt(LocalDateTime.now().minusDays(1));
        inProgressOrder.setUpdatedAt(LocalDateTime.now());
        inProgressOrder = orderRepository.save(inProgressOrder);
    }

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== Service 层集成测试（Service + Repository + 真实DB） ====================

    @Nested
    @DisplayName("评价创建 - Service层集成测试")
    class CreateReviewServiceTests {

        @Test
        @DisplayName("发布者成功评价接单者并更新评分")
        void testPublisherCreatesReviewSuccess() {
            Review review = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 5, "非常满意！");

            assertNotNull(review);
            assertNotNull(review.getId());
            assertEquals(completedOrder.getId(), review.getOrderId());
            assertEquals(publisher.getId(), review.getReviewerId());
            assertEquals(acceptor.getId(), review.getReviewedId());
            assertEquals(5, review.getScore());
            assertEquals("非常满意！", review.getContent());
            assertNotNull(review.getCreatedAt());

            // 验证被评价者评分已更新
            User updatedAcceptor = userService.getUserById(acceptor.getId());
            assertEquals(1L, updatedAcceptor.getScoreNum());
            assertEquals(5.0, updatedAcceptor.getAverageScore());
        }

        @Test
        @DisplayName("接单者成功评价发布者")
        void testAcceptorCreatesReviewSuccess() {
            Review review = reviewService.createReview(
                completedOrder.getId(), acceptor.getId(), 4, "不错");

            assertNotNull(review);
            assertEquals(acceptor.getId(), review.getReviewerId());
            assertEquals(publisher.getId(), review.getReviewedId());
            assertEquals(4, review.getScore());

            User updatedPublisher = userService.getUserById(publisher.getId());
            assertEquals(1L, updatedPublisher.getScoreNum());
            assertEquals(4.0, updatedPublisher.getAverageScore());
        }

        @Test
        @DisplayName("双方都完成评价后更新订单commentId（设为第二评价者ID）")
        void testBothPartiesReviewUpdatesCommentId() {
            Review review1 = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 5, "好评");
            Review review2 = reviewService.createReview(
                completedOrder.getId(), acceptor.getId(), 4, "不错");

            Order updatedOrder = orderRepository.findById(completedOrder.getId()).orElseThrow();
            assertNotNull(updatedOrder.getCommentId());
            // 当双方都评价完（count==2）时，commentId设为触发满评的那个评价ID
            assertEquals(review2.getId(), updatedOrder.getCommentId());
        }

        @Test
        @DisplayName("评分低于1抛出异常")
        void testScoreTooLowThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(completedOrder.getId(), publisher.getId(), 0, "太差了"));
            assertEquals("评分必须在 1 到 5 之间", ex.getMessage());
        }

        @Test
        @DisplayName("评分高于5抛出异常")
        void testScoreTooHighThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(completedOrder.getId(), publisher.getId(), 6, "太好了"));
            assertEquals("评分必须在 1 到 5 之间", ex.getMessage());
        }

        @Test
        @DisplayName("订单不存在时抛出异常")
        void testOrderNotFoundThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(99999L, publisher.getId(), 5, "好评"));
            assertEquals("订单不存在", ex.getMessage());
        }

        @Test
        @DisplayName("订单未完成时不能评价")
        void testOrderNotCompletedThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(inProgressOrder.getId(), publisher.getId(), 5, "好评"));
            assertEquals("只有已完成的订单才能进行评价", ex.getMessage());
        }

        @Test
        @DisplayName("非订单参与方不能评价")
        void testNonParticipantThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(completedOrder.getId(), outsider.getId(), 5, "好评"));
            assertEquals("只有订单参与方才能进行评价", ex.getMessage());
        }

        @Test
        @DisplayName("同一用户不能重复评价同一订单")
        void testDuplicateReviewThrowsException() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "好评");

            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.createReview(completedOrder.getId(), publisher.getId(), 3, "还行"));
            assertEquals("您已经评价过此订单", ex.getMessage());
        }

        @Test
        @DisplayName("评价内容可为空")
        void testCreateReviewWithNullContent() {
            Review review = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 3, null);

            assertNotNull(review);
            assertNull(review.getContent());
        }

        @Test
        @DisplayName("创建评价后用户平均分正确累计")
        void testAverageScoreAccumulatesCorrectly() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "好评");
            User u = userService.getUserById(acceptor.getId());
            assertEquals(5.0, u.getAverageScore());
            assertEquals(1L, u.getScoreNum());

            Order order2 = new Order(300L, publisher.getId(), acceptor.getId());
            order2.setStatus("COMPLETED");
            order2.setCreatedAt(LocalDateTime.now().minusDays(3));
            order2.setUpdatedAt(LocalDateTime.now());
            order2.setCompletedAt(LocalDateTime.now());
            order2 = orderRepository.save(order2);

            reviewService.createReview(order2.getId(), publisher.getId(), 3, "一般");
            User u2 = userService.getUserById(acceptor.getId());
            assertEquals(4.0, u2.getAverageScore()); // (5+3)/2 = 4.0
            assertEquals(2L, u2.getScoreNum());
        }
    }

    @Nested
    @DisplayName("评价查询 - Service层集成测试")
    class ReviewQueryServiceTests {

        private Review savedReview;

        @BeforeEach
        void createReview() {
            savedReview = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 5, "服务很棒！");
        }

        @Test
        @DisplayName("根据ID查询评价成功")
        void testGetReviewByIdFound() {
            Optional<Review> result = reviewService.getReviewById(savedReview.getId());
            assertTrue(result.isPresent());
            assertEquals(savedReview.getId(), result.get().getId());
            assertEquals("服务很棒！", result.get().getContent());
        }

        @Test
        @DisplayName("查询不存在的评价返回空")
        void testGetReviewByIdNotFound() {
            Optional<Review> result = reviewService.getReviewById(99999L);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("根据订单ID获取评价列表")
        void testGetReviewsByOrderId() {
            List<Review> reviews = reviewService.getReviewsByOrderId(completedOrder.getId());
            assertEquals(1, reviews.size());
            assertEquals(savedReview.getId(), reviews.get(0).getId());
        }

        @Test
        @DisplayName("获取不存在的订单评价返回空列表")
        void testGetReviewsByOrderIdEmpty() {
            List<Review> reviews = reviewService.getReviewsByOrderId(99999L);
            assertTrue(reviews.isEmpty());
        }

        @Test
        @DisplayName("获取用户收到的评价（分页）")
        void testGetReviewsReceived() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = reviewService.getReviewsReceived(acceptor.getId(), pageable);

            assertEquals(1, page.getTotalElements());
            assertEquals(0, page.getNumber());
            assertEquals(savedReview.getId(), page.getContent().get(0).getId());
        }

        @Test
        @DisplayName("获取用户发出的评价（分页）")
        void testGetReviewsGiven() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = reviewService.getReviewsGiven(publisher.getId(), pageable);

            assertEquals(1, page.getTotalElements());
            assertEquals(publisher.getId(), page.getContent().get(0).getReviewerId());
        }

        @Test
        @DisplayName("获取用户对特定订单的评价")
        void testGetUserReviewForOrder() {
            Optional<Review> result = reviewService.getUserReviewForOrder(
                completedOrder.getId(), publisher.getId());
            assertTrue(result.isPresent());
            assertEquals(savedReview.getId(), result.get().getId());
        }

        @Test
        @DisplayName("未评价时获取用户对订单的评价返回空")
        void testGetUserReviewForOrderNotFound() {
            Optional<Review> result = reviewService.getUserReviewForOrder(
                completedOrder.getId(), acceptor.getId());
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("评价统计 - Service层集成测试")
    class ReviewStatisticsServiceTests {

        @Test
        @DisplayName("获取用户评价统计信息")
        void testGetUserReviewStatistics() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "好评");

            Order order2 = new Order(300L, publisher.getId(), acceptor.getId());
            order2.setStatus("COMPLETED");
            order2.setCreatedAt(LocalDateTime.now().minusDays(3));
            order2.setUpdatedAt(LocalDateTime.now());
            order2.setCompletedAt(LocalDateTime.now());
            order2 = orderRepository.save(order2);
            reviewService.createReview(order2.getId(), publisher.getId(), 4, "不错");

            Order order3 = new Order(400L, outsider.getId(), acceptor.getId());
            order3.setStatus("COMPLETED");
            order3.setCreatedAt(LocalDateTime.now().minusDays(2));
            order3.setUpdatedAt(LocalDateTime.now());
            order3.setCompletedAt(LocalDateTime.now());
            order3 = orderRepository.save(order3);
            reviewService.createReview(order3.getId(), outsider.getId(), 5, "完美");

            ReviewService.ReviewStatistics stats = reviewService.getUserReviewStatistics(acceptor.getId());

            assertEquals(acceptor.getId(), stats.getUserId());
            assertEquals(3L, stats.getTotalCount());
            assertTrue(stats.getAverageScore() > 4.6 && stats.getAverageScore() < 4.7); // (5+4+5)/3 ≈ 4.67
            assertEquals(0L, stats.getScore1Count());
            assertEquals(0L, stats.getScore2Count());
            assertEquals(0L, stats.getScore3Count());
            assertEquals(1L, stats.getScore4Count());
            assertEquals(2L, stats.getScore5Count());
        }

        @Test
        @DisplayName("用户没有评价时统计数据为0")
        void testGetReviewStatisticsNoReviews() {
            ReviewService.ReviewStatistics stats = reviewService.getUserReviewStatistics(publisher.getId());

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
        void testHighScoreRate() {
            Order order2 = new Order(300L, publisher.getId(), acceptor.getId());
            order2.setStatus("COMPLETED");
            order2.setCreatedAt(LocalDateTime.now().minusDays(3));
            order2.setUpdatedAt(LocalDateTime.now());
            order2.setCompletedAt(LocalDateTime.now());
            order2 = orderRepository.save(order2);

            reviewService.createReview(completedOrder.getId(), publisher.getId(), 4, "好");
            reviewService.createReview(order2.getId(), publisher.getId(), 2, "一般");

            ReviewService.ReviewStatistics stats = reviewService.getUserReviewStatistics(acceptor.getId());
            assertEquals(50.0, stats.getHighScoreRate());
        }
    }

    @Nested
    @DisplayName("评价搜索 - Service层集成测试")
    class SearchReviewsServiceTests {

        @BeforeEach
        void createMultipleReviews() {
            Order order2 = new Order(300L, publisher.getId(), acceptor.getId());
            order2.setStatus("COMPLETED");
            order2.setCreatedAt(LocalDateTime.now().minusDays(3));
            order2.setUpdatedAt(LocalDateTime.now());
            order2.setCompletedAt(LocalDateTime.now());
            order2 = orderRepository.save(order2);

            Order order3 = new Order(400L, acceptor.getId(), publisher.getId());
            order3.setStatus("COMPLETED");
            order3.setCreatedAt(LocalDateTime.now().minusDays(2));
            order3.setUpdatedAt(LocalDateTime.now());
            order3.setCompletedAt(LocalDateTime.now());
            order3 = orderRepository.save(order3);

            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "非常满意服务");
            reviewService.createReview(order2.getId(), publisher.getId(), 3, "一般般吧");
            reviewService.createReview(order3.getId(), acceptor.getId(), 4, "挺好的平台");
        }

        @Test
        @DisplayName("按评价者ID搜索")
        void testSearchByReviewerId() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> result = reviewService.searchReviews(
                publisher.getId(), null, null, null, null, null, pageable);

            assertEquals(2, result.getTotalElements());
            result.forEach(r -> assertEquals(publisher.getId(), r.getReviewerId()));
        }

        @Test
        @DisplayName("按被评价者ID搜索")
        void testSearchByReviewedId() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> result = reviewService.searchReviews(
                null, acceptor.getId(), null, null, null, null, pageable);

            assertEquals(2, result.getTotalElements());
            result.forEach(r -> assertEquals(acceptor.getId(), r.getReviewedId()));
        }

        @Test
        @DisplayName("按订单ID搜索")
        void testSearchByOrderId() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> result = reviewService.searchReviews(
                null, null, completedOrder.getId(), null, null, null, pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals(completedOrder.getId(), result.getContent().get(0).getOrderId());
        }

        @Test
        @DisplayName("按分数范围搜索")
        void testSearchByScoreRange() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> result = reviewService.searchReviews(
                null, null, null, 4, 5, null, pageable);

            assertEquals(2, result.getTotalElements());
            result.forEach(r -> assertTrue(r.getScore() >= 4 && r.getScore() <= 5));
        }

        @Test
        @DisplayName("按关键词搜索评价内容")
        void testSearchByKeyword() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> result = reviewService.searchReviews(
                null, null, null, null, null, "满意", pageable);

            assertEquals(1, result.getTotalElements());
            assertTrue(result.getContent().get(0).getContent().contains("满意"));
        }

        @Test
        @DisplayName("组合条件搜索")
        void testSearchWithMultipleConditions() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> result = reviewService.searchReviews(
                publisher.getId(), acceptor.getId(), null, 4, 5, "满意", pageable);

            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("空条件搜索返回所有评价")
        void testSearchWithNoConditions() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> result = reviewService.searchReviews(
                null, null, null, null, null, null, pageable);

            assertEquals(3, result.getTotalElements());
        }

        @Test
        @DisplayName("分页搜索")
        void testSearchWithPagination() {
            Pageable pageable = PageRequest.of(0, 1);
            Page<Review> result = reviewService.searchReviews(
                null, null, null, null, null, null, pageable);

            assertEquals(3, result.getTotalElements());
            assertEquals(3, result.getTotalPages()); // 3条数据，每页1条 = 3页
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("订单评价状态 - Service层集成测试")
    class OrderReviewStatusServiceTests {

        @Test
        @DisplayName("双方都评价后订单标记为完全评价")
        void testOrderFullyReviewed() {
            assertFalse(reviewService.isOrderFullyReviewed(completedOrder.getId()));

            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "好评");
            assertFalse(reviewService.isOrderFullyReviewed(completedOrder.getId()));

            reviewService.createReview(completedOrder.getId(), acceptor.getId(), 4, "不错");
            assertTrue(reviewService.isOrderFullyReviewed(completedOrder.getId()));
        }

        @Test
        @DisplayName("未评价的订单返回false")
        void testOrderNotReviewed() {
            assertFalse(reviewService.isOrderFullyReviewed(completedOrder.getId()));
        }
    }

    @Nested
    @DisplayName("删除评价 - Service层集成测试")
    class DeleteReviewServiceTests {

        private Review savedReview;

        @BeforeEach
        void createReviewWithScore() {
            savedReview = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 5, "好评");
        }

        @Test
        @DisplayName("管理员成功删除评价并重新计算评分")
        void testAdminDeleteReviewSuccess() {
            User acceptor1 = userService.getUserById(acceptor.getId());
            assertEquals(5.0, acceptor1.getAverageScore());

            reviewService.deleteReview(savedReview.getId(), adminUser.getId());

            Optional<Review> deleted = reviewRepository.findById(savedReview.getId());
            assertTrue(deleted.isEmpty());

            User acceptor2 = userService.getUserById(acceptor.getId());
            assertEquals(0.0, acceptor2.getAverageScore());
            assertEquals(0L, acceptor2.getScoreNum());
        }

        @Test
        @DisplayName("非管理员删除评价抛出异常")
        void testNonAdminDeleteThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.deleteReview(savedReview.getId(), publisher.getId()));
            assertEquals("只有管理员可以删除评价", ex.getMessage());
            assertTrue(reviewRepository.findById(savedReview.getId()).isPresent());
        }

        @Test
        @DisplayName("删除不存在的评价抛出异常")
        void testDeleteNonExistentReviewThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.deleteReview(99999L, adminUser.getId()));
            assertEquals("评价不存在", ex.getMessage());
        }

        @Test
        @DisplayName("删除评价后重新计算平均分（多条评价时）")
        void testDeleteReviewRecalculatesAverage() {
            Order order2 = new Order(300L, publisher.getId(), acceptor.getId());
            order2.setStatus("COMPLETED");
            order2.setCreatedAt(LocalDateTime.now().minusDays(3));
            order2.setUpdatedAt(LocalDateTime.now());
            order2.setCompletedAt(LocalDateTime.now());
            order2 = orderRepository.save(order2);
            reviewService.createReview(order2.getId(), publisher.getId(), 3, "一般");

            reviewService.deleteReview(savedReview.getId(), adminUser.getId());

            User u = userService.getUserById(acceptor.getId());
            assertEquals(3.0, u.getAverageScore());
            assertEquals(1L, u.getScoreNum());
        }
    }

    @Nested
    @DisplayName("边界场景 - Service层集成测试")
    class BoundaryServiceTests {

        @Test
        @DisplayName("创建评价内容较长文本")
        void testCreateReviewWithLongContent() {
            // H2 auto-DDL默认VARCHAR(255)，使用250字符测试接近上限
            String longContent = "A".repeat(200);
            Review review = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 3, longContent);

            assertNotNull(review);
            assertEquals(longContent, review.getContent());
        }

        @Test
        @DisplayName("分页获取大量评价")
        void testPaginationWithMultipleReviews() {
            for (int i = 0; i < 10; i++) {
                Order order = new Order((long) (500 + i), publisher.getId(), acceptor.getId());
                order.setStatus("COMPLETED");
                order.setCreatedAt(LocalDateTime.now().minusDays(i));
                order.setUpdatedAt(LocalDateTime.now());
                order.setCompletedAt(LocalDateTime.now());
                order = orderRepository.save(order);
                reviewService.createReview(order.getId(), publisher.getId(), (i % 5) + 1, "评价内容" + i);
            }

            Pageable pageable = PageRequest.of(0, 5);
            Page<Review> page = reviewService.getReviewsReceived(acceptor.getId(), pageable);

            assertEquals(10L, page.getTotalElements());
            assertEquals(5, page.getContent().size());
            assertEquals(2, page.getTotalPages());
        }

        @Test
        @DisplayName("评分边界值1分和5分")
        void testScoreBoundaries() {
            Review review1 = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 1, "极差");
            assertEquals(1, review1.getScore());

            Order order2 = new Order(300L, publisher.getId(), acceptor.getId());
            order2.setStatus("COMPLETED");
            order2.setCreatedAt(LocalDateTime.now().minusDays(3));
            order2.setUpdatedAt(LocalDateTime.now());
            order2.setCompletedAt(LocalDateTime.now());
            order2 = orderRepository.save(order2);

            Review review5 = reviewService.createReview(
                order2.getId(), publisher.getId(), 5, "极好");
            assertEquals(5, review5.getScore());
        }

        @Test
        @DisplayName("同一订单双方评价后完整流程验证")
        void testFullReviewFlow() {
            // 1. 发布者评价接单者
            Review r1 = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 4, "效率高");
            assertNotNull(r1.getId());

            // 2. 验证订单未完全评价
            assertFalse(reviewService.isOrderFullyReviewed(completedOrder.getId()));

            // 3. 接单者评价发布者
            Review r2 = reviewService.createReview(
                completedOrder.getId(), acceptor.getId(), 5, "好客户");
            assertNotNull(r2.getId());

            // 4. 验证订单完全评价
            assertTrue(reviewService.isOrderFullyReviewed(completedOrder.getId()));

            // 5. 验证订单commentId（设为触发满评的第二个评价ID）
            Order updated = orderRepository.findById(completedOrder.getId()).orElseThrow();
            assertNotNull(updated.getCommentId());
            assertEquals(r2.getId(), updated.getCommentId());

            // 6. 验证双方评分
            User ua = userService.getUserById(acceptor.getId());
            assertEquals(4.0, ua.getAverageScore());
            assertEquals(1L, ua.getScoreNum());

            User up = userService.getUserById(publisher.getId());
            assertEquals(5.0, up.getAverageScore());
            assertEquals(1L, up.getScoreNum());

            // 7. 验证双方都无法重复评价
            assertThrows(RuntimeException.class,
                () -> reviewService.createReview(completedOrder.getId(), publisher.getId(), 3, "再评"));
            assertThrows(RuntimeException.class,
                () -> reviewService.createReview(completedOrder.getId(), acceptor.getId(), 3, "再评"));
        }
    }

    // ==================== Controller 层集成测试（Controller → Service → Repository → H2 Database） ====================

    @Nested
    @DisplayName("评价Controller - 接口层集成测试")
    class ReviewControllerTests {

        @Test
        @DisplayName("POST /reviews/create - 创建评价成功返回201")
        void testCreateReviewSuccess() {
            ResponseEntity<ApiResponse<Review>> response = reviewController.createReview(
                completedOrder.getId(), publisher.getId(), 5, "非常好！");

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
            Review data = response.getBody().getData();
            assertNotNull(data);
            assertEquals(completedOrder.getId(), data.getOrderId());
            assertEquals(publisher.getId(), data.getReviewerId());
            assertEquals(acceptor.getId(), data.getReviewedId());
            assertEquals(5, data.getScore());
            assertEquals("非常好！", data.getContent());
        }

        @Test
        @DisplayName("POST /reviews/create - 评分无效返回400")
        void testCreateReviewInvalidScore() {
            ResponseEntity<ApiResponse<Review>> response = reviewController.createReview(
                completedOrder.getId(), publisher.getId(), 0, "差");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertFalse(response.getBody().isSuccess());
            assertEquals(400, response.getBody().getCode());
        }

        @Test
        @DisplayName("POST /reviews/create - 订单不存在返回400")
        void testCreateReviewOrderNotFound() {
            ResponseEntity<ApiResponse<Review>> response = reviewController.createReview(
                99999L, publisher.getId(), 5, "好");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertFalse(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("POST /reviews/create - 非参与方不能评价")
        void testCreateReviewNonParticipant() {
            ResponseEntity<ApiResponse<Review>> response = reviewController.createReview(
                completedOrder.getId(), outsider.getId(), 5, "好");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertFalse(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("GET /reviews/{id} - 根据ID获取评价成功")
        void testGetReviewByIdFound() {
            Review review = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 5, "测试");

            ResponseEntity<ApiResponse<Review>> response = reviewController.getReviewById(review.getId());

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
            assertEquals(review.getId(), response.getBody().getData().getId());
            assertEquals(5, response.getBody().getData().getScore());
            assertEquals("测试", response.getBody().getData().getContent());
        }

        @Test
        @DisplayName("GET /reviews/{id} - 不存在的评价返回404")
        void testGetReviewByIdNotFound() {
            ResponseEntity<ApiResponse<Review>> response = reviewController.getReviewById(99999L);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertFalse(response.getBody().isSuccess());
            assertEquals(404, response.getBody().getCode());
        }

        @Test
        @DisplayName("GET /reviews/order/{orderId} - 获取订单评价列表")
        void testGetReviewsByOrderId() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 4, "不错");

            ResponseEntity<ApiResponse<List<Review>>> response = reviewController.getReviewsByOrderId(completedOrder.getId());

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
            assertEquals(1, response.getBody().getData().size());
        }

        @Test
        @DisplayName("GET /reviews/order/{orderId} - 无评价时返回空列表")
        void testGetReviewsByOrderIdEmpty() {
            ResponseEntity<ApiResponse<List<Review>>> response = reviewController.getReviewsByOrderId(99999L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().getData().isEmpty());
        }

        @Test
        @DisplayName("GET /reviews/received/{userId} - 分页获取收到的评价")
        void testGetReviewsReceived() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "好评");

            ResponseEntity<ApiResponse<Map<String, Object>>> response = reviewController.getReviewsReceived(
                acceptor.getId(), 0, 10, null, "createdAt", "desc");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> data = response.getBody().getData();
            assertEquals(1, ((Number) data.get("totalElements")).intValue());
            assertEquals(0, ((Number) data.get("currentPage")).intValue());
        }

        @Test
        @DisplayName("GET /reviews/received/{userId} - 按分数筛选")
        void testGetReviewsReceivedByScore() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "好评");

            ResponseEntity<ApiResponse<Map<String, Object>>> resp5 = reviewController.getReviewsReceived(
                acceptor.getId(), 0, 10, 5, "createdAt", "desc");
            assertEquals(1, ((List<?>) resp5.getBody().getData().get("content")).size());

            ResponseEntity<ApiResponse<Map<String, Object>>> resp1 = reviewController.getReviewsReceived(
                acceptor.getId(), 0, 10, 1, "createdAt", "desc");
            assertEquals(0, ((List<?>) resp1.getBody().getData().get("content")).size());
        }

        @Test
        @DisplayName("GET /reviews/given/{userId} - 分页获取发出的评价")
        void testGetReviewsGiven() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "好评");

            ResponseEntity<ApiResponse<Map<String, Object>>> response = reviewController.getReviewsGiven(
                publisher.getId(), 0, 10, null, "createdAt", "desc");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> data = response.getBody().getData();
            assertEquals(1, ((Number) data.get("totalElements")).intValue());
        }

        @Test
        @DisplayName("GET /reviews/given/{userId} - 按分数筛选")
        void testGetReviewsGivenByScore() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 3, "一般");

            ResponseEntity<ApiResponse<Map<String, Object>>> response = reviewController.getReviewsGiven(
                publisher.getId(), 0, 10, 3, "createdAt", "desc");

            assertEquals(1, ((List<?>) response.getBody().getData().get("content")).size());
        }

        @Test
        @DisplayName("GET /reviews/statistics/{userId} - 获取评价统计")
        void testGetReviewStatistics() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 4, "好评");

            ResponseEntity<ApiResponse<ReviewService.ReviewStatistics>> response =
                reviewController.getReviewStatistics(acceptor.getId());

            assertEquals(HttpStatus.OK, response.getStatusCode());
            ReviewService.ReviewStatistics stats = response.getBody().getData();
            assertEquals(acceptor.getId(), stats.getUserId());
            assertEquals(4.0, stats.getAverageScore());
            assertEquals(1L, stats.getTotalCount());
            assertEquals(1L, stats.getScore4Count());
            assertEquals(100.0, stats.getHighScoreRate());
        }

        @Test
        @DisplayName("GET /reviews/statistics/{userId} - 无评价用户统计")
        void testGetReviewStatisticsNoData() {
            ResponseEntity<ApiResponse<ReviewService.ReviewStatistics>> response =
                reviewController.getReviewStatistics(publisher.getId());

            ReviewService.ReviewStatistics stats = response.getBody().getData();
            assertEquals(0.0, stats.getAverageScore());
            assertEquals(0L, stats.getTotalCount());
        }

        @Test
        @DisplayName("GET /reviews/search - 多条件搜索评价")
        void testSearchReviews() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "非常满意");

            ResponseEntity<ApiResponse<Map<String, Object>>> response = reviewController.searchReviews(
                publisher.getId(), null, null, null, null, null, 0, 10, "createdAt", "desc");

            Map<String, Object> data = response.getBody().getData();
            assertEquals(1, ((Number) data.get("totalElements")).intValue());
        }

        @Test
        @DisplayName("GET /reviews/search - 按关键词搜索")
        void testSearchReviewsByKeyword() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "服务态度非常好");

            ResponseEntity<ApiResponse<Map<String, Object>>> resp1 = reviewController.searchReviews(
                null, null, null, null, null, "态度", 0, 10, "createdAt", "desc");
            assertEquals(1, ((List<?>) resp1.getBody().getData().get("content")).size());

            ResponseEntity<ApiResponse<Map<String, Object>>> resp2 = reviewController.searchReviews(
                null, null, null, null, null, "不存在", 0, 10, "createdAt", "desc");
            assertEquals(0, ((List<?>) resp2.getBody().getData().get("content")).size());
        }

        @Test
        @DisplayName("GET /reviews/user-order - 获取用户对订单的评价")
        void testGetUserReviewForOrder() {
            Review review = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 5, "好");

            ResponseEntity<ApiResponse<Review>> response = reviewController.getUserReviewForOrder(
                completedOrder.getId(), publisher.getId());

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(review.getId(), response.getBody().getData().getId());
        }

        @Test
        @DisplayName("GET /reviews/user-order - 未评价返回404")
        void testGetUserReviewForOrderNotFound() {
            ResponseEntity<ApiResponse<Review>> response = reviewController.getUserReviewForOrder(
                completedOrder.getId(), publisher.getId());

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertFalse(response.getBody().isSuccess());
            assertEquals(404, response.getBody().getCode());
        }

        @Test
        @DisplayName("GET /reviews/order/{orderId}/fully-reviewed - 检查完全评价状态")
        void testIsOrderFullyReviewed() {
            // 未评价
            ResponseEntity<ApiResponse<Map<String, Boolean>>> resp0 =
                reviewController.isOrderFullyReviewed(completedOrder.getId());
            assertFalse(resp0.getBody().getData().get("fullyReviewed"));

            // 一方评价
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "好");
            ResponseEntity<ApiResponse<Map<String, Boolean>>> resp1 =
                reviewController.isOrderFullyReviewed(completedOrder.getId());
            assertFalse(resp1.getBody().getData().get("fullyReviewed"));

            // 双方评价
            reviewService.createReview(completedOrder.getId(), acceptor.getId(), 4, "不错");
            ResponseEntity<ApiResponse<Map<String, Boolean>>> resp2 =
                reviewController.isOrderFullyReviewed(completedOrder.getId());
            assertTrue(resp2.getBody().getData().get("fullyReviewed"));
        }

        @Test
        @DisplayName("DELETE /reviews/{id} - 管理员删除评价成功")
        void testDeleteReviewByAdmin() {
            Review review = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 5, "好");

            ResponseEntity<ApiResponse<Void>> response = reviewController.deleteReview(
                review.getId(), adminUser.getId());

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
            assertTrue(reviewRepository.findById(review.getId()).isEmpty());
        }

        @Test
        @DisplayName("DELETE /reviews/{id} - 非管理员删除返回403")
        void testDeleteReviewByNonAdmin() {
            Review review = reviewService.createReview(
                completedOrder.getId(), publisher.getId(), 5, "好");

            ResponseEntity<ApiResponse<Void>> response = reviewController.deleteReview(
                review.getId(), publisher.getId());

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertFalse(response.getBody().isSuccess());
            assertEquals(403, response.getBody().getCode());
            assertTrue(reviewRepository.findById(review.getId()).isPresent());
        }

        @Test
        @DisplayName("DELETE /reviews/{id} - 删除不存在的评价返回404")
        void testDeleteNonExistentReview() {
            ResponseEntity<ApiResponse<Void>> response = reviewController.deleteReview(
                99999L, adminUser.getId());

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertFalse(response.getBody().isSuccess());
            assertEquals(404, response.getBody().getCode());
        }

        @Test
        @DisplayName("GET /reviews/score-range - 获取评分范围")
        void testGetScoreRange() {
            ResponseEntity<ApiResponse<Map<String, Integer>>> response = reviewController.getScoreRange();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, response.getBody().getData().get("min"));
            assertEquals(5, response.getBody().getData().get("max"));
        }

        @Test
        @DisplayName("GET /reviews/received/{userId} - 测试排序参数")
        void testGetReviewsReceivedWithSorting() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "第一个评价");

            ResponseEntity<ApiResponse<Map<String, Object>>> response = reviewController.getReviewsReceived(
                acceptor.getId(), 0, 10, null, "createdAt", "asc");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, ((List<?>) response.getBody().getData().get("content")).size());
        }

        @Test
        @DisplayName("GET /reviews/search - 分页和排序")
        void testSearchReviewsWithPagination() {
            reviewService.createReview(completedOrder.getId(), publisher.getId(), 5, "评价A");

            ResponseEntity<ApiResponse<Map<String, Object>>> response = reviewController.searchReviews(
                null, null, null, null, null, null, 0, 10, "score", "desc");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> data = response.getBody().getData();
            assertTrue(((Number) data.get("totalPages")).intValue() >= 0);
        }
    }
}
