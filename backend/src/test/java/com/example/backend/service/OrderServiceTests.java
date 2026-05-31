package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.backend.entity.Demand;
import com.example.backend.entity.Order;
import com.example.backend.entity.User;
import com.example.backend.repository.DemandRepository;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务单元测试")
class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DemandRepository demandRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DemandService demandService;

    @InjectMocks
    private OrderService orderService;

    private User testPublisher;
    private User testAcceptor;
    private Demand testDemand;
    private Order testOrder;
    private final Long TEST_DEMAND_ID = 100L;
    private final Long TEST_PUBLISHER_ID = 1000L;
    private final Long TEST_ACCEPTOR_ID = 2000L;
    private final Long TEST_ORDER_ID = 5000L;
    private final Long TEST_COMMENT_ID = 8000L;

    @BeforeEach
    void setUp() {
        testPublisher = new User("发布者", "13800138000", "password");
        testPublisher.setId(TEST_PUBLISHER_ID);

        testAcceptor = new User("接单者", "13900139000", "password");
        testAcceptor.setId(TEST_ACCEPTOR_ID);

        testDemand = new Demand();
        testDemand.setId(TEST_DEMAND_ID);
        testDemand.setTitle("测试需求");
        testDemand.setDescription("需要帮忙修理电脑");
        testDemand.setCategory("IT支持");
        testDemand.setPublisherId(TEST_PUBLISHER_ID);
        testDemand.setStatus("PENDING");
        testDemand.setLocation("上海市浦东新区");
        testDemand.setDeadline(LocalDateTime.now().plusDays(7));
        testDemand.setReward(500.0);
        testDemand.setCreatedAt(LocalDateTime.now());
        testDemand.setUpdatedAt(LocalDateTime.now());
        testDemand.setOrderId(null);

        testOrder = new Order(TEST_DEMAND_ID, TEST_PUBLISHER_ID, TEST_ACCEPTOR_ID);
        testOrder.setId(TEST_ORDER_ID);
        testOrder.setStatus("ACCEPTED");
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());
        testOrder.setCompletedAt(null);
        testOrder.setLatestRequesterNote(null);
        testOrder.setCommentId(null);
    }

    // ==================== 创建订单测试 ====================

    @Nested
    @DisplayName("创建订单测试")
    class CreateOrderTests {

        @Test
        @DisplayName("成功创建订单")
        void testCreateOrderSuccess() {
            // Given
            when(demandService.getDemandById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(orderRepository.findByDemandId(TEST_DEMAND_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(TEST_ACCEPTOR_ID)).thenReturn(Optional.of(testAcceptor));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(TEST_ORDER_ID);
                return saved;
            });
            when(demandService.updateDemandStatus(TEST_DEMAND_ID, "ACCEPTED")).thenReturn(true);
            when(demandService.updateOrderId(TEST_DEMAND_ID, TEST_ORDER_ID)).thenReturn(true);

            // When
            Order result = orderService.createOrder(TEST_DEMAND_ID, TEST_ACCEPTOR_ID);

            // Then
            assertNotNull(result);
            assertEquals(TEST_DEMAND_ID, result.getDemandId());
            assertEquals(TEST_PUBLISHER_ID, result.getPublisherId());
            assertEquals(TEST_ACCEPTOR_ID, result.getAcceptorId());
            assertEquals("ACCEPTED", result.getStatus());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());

            verify(demandService).updateDemandStatus(TEST_DEMAND_ID, "ACCEPTED");
            verify(demandService).updateOrderId(TEST_DEMAND_ID, TEST_ORDER_ID);
        }

        @Test
        @DisplayName("需求不存在时创建订单失败")
        void testCreateOrderDemandNotFound() {
            // Given
            when(demandService.getDemandById(TEST_DEMAND_ID)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(TEST_DEMAND_ID, TEST_ACCEPTOR_ID));
            assertEquals("需求不存在", exception.getMessage());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("需求状态不是PENDING时无法接单")
        void testCreateOrderDemandNotPending() {
            // Given
            testDemand.setStatus("ACCEPTED");
            when(demandService.getDemandById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(TEST_DEMAND_ID, TEST_ACCEPTOR_ID));
            assertEquals("该需求当前状态为ACCEPTED，无法接单", exception.getMessage());
        }

        @Test
        @DisplayName("发布者不能接自己的单")
        void testCreateOrderPublisherCannotAcceptOwnDemand() {
            // Given
            when(demandService.getDemandById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(TEST_DEMAND_ID, TEST_PUBLISHER_ID));
            assertEquals("不能接自己发布的需求", exception.getMessage());
        }

        @Test
        @DisplayName("需求已有订单时无法重复接单")
        void testCreateOrderDemandAlreadyHasOrder() {
            // Given
            when(demandService.getDemandById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(orderRepository.findByDemandId(TEST_DEMAND_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(TEST_DEMAND_ID, TEST_ACCEPTOR_ID));
            assertEquals("该需求已被接单", exception.getMessage());
        }

        @Test
        @DisplayName("接单者不存在时创建订单失败")
        void testCreateOrderAcceptorNotFound() {
            // Given
            when(demandService.getDemandById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(orderRepository.findByDemandId(TEST_DEMAND_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(TEST_ACCEPTOR_ID)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(TEST_DEMAND_ID, TEST_ACCEPTOR_ID));
            assertEquals("用户不存在", exception.getMessage());
        }
    }

    // ==================== 订单查询测试 ====================

    @Nested
    @DisplayName("订单查询测试")
    class OrderQueryTests {

        @Test
        @DisplayName("根据ID获取订单成功")
        void testGetOrderByIdSuccess() {
            // Given
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When
            Optional<Order> result = orderService.getOrderById(TEST_ORDER_ID);

            // Then
            assertTrue(result.isPresent());
            assertEquals(TEST_ORDER_ID, result.get().getId());
        }

        @Test
        @DisplayName("根据需求ID获取订单成功")
        void testGetOrderByDemandIdSuccess() {
            // Given
            when(orderRepository.findByDemandId(TEST_DEMAND_ID)).thenReturn(Optional.of(testOrder));

            // When
            Optional<Order> result = orderService.getOrderByDemandId(TEST_DEMAND_ID);

            // Then
            assertTrue(result.isPresent());
            assertEquals(TEST_DEMAND_ID, result.get().getDemandId());
        }

        @Test
        @DisplayName("获取不存在的订单返回空")
        void testGetOrderByIdNotFound() {
            // Given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<Order> result = orderService.getOrderById(999L);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 订单状态更新测试 ====================

    @Nested
    @DisplayName("订单状态更新测试")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("ACCEPTED -> IN_PROGRESS 发布者更新成功")
        void testUpdateStatusAcceptedToInProgressByPublisher() {
            // Given
            testOrder.setStatus("ACCEPTED");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(demandService.updateDemandStatus(TEST_DEMAND_ID, "ACCEPTED")).thenReturn(true);

            // When
            Order result = orderService.updateOrderStatus(TEST_ORDER_ID, TEST_PUBLISHER_ID, "IN_PROGRESS", null);

            // Then
            assertEquals("IN_PROGRESS", result.getStatus());
            assertNotNull(result.getUpdatedAt());
            verify(orderRepository).save(testOrder);
        }

        @Test
        @DisplayName("ACCEPTED -> IN_PROGRESS 接单者更新成功")
        void testUpdateStatusAcceptedToInProgressByAcceptor() {
            // Given
            testOrder.setStatus("ACCEPTED");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(demandService.updateDemandStatus(TEST_DEMAND_ID, "ACCEPTED")).thenReturn(true);

            // When
            Order result = orderService.updateOrderStatus(TEST_ORDER_ID, TEST_ACCEPTOR_ID, "IN_PROGRESS", null);

            // Then
            assertEquals("IN_PROGRESS", result.getStatus());
        }

        @Test
        @DisplayName("IN_PROGRESS -> COMPLETED 发布者更新成功")
        void testUpdateStatusInProgressToCompleted() {
            // Given
            testOrder.setStatus("IN_PROGRESS");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(demandService.updateDemandStatus(TEST_DEMAND_ID, "COMPLETED")).thenReturn(true);

            // When
            Order result = orderService.updateOrderStatus(TEST_ORDER_ID, TEST_PUBLISHER_ID, "COMPLETED", null);

            // Then
            assertEquals("COMPLETED", result.getStatus());
            assertNotNull(result.getCompletedAt());
        }

        @Test
        @DisplayName("IN_PROGRESS -> DISPUTED 更新成功")
        void testUpdateStatusInProgressToDisputed() {
            // Given
            testOrder.setStatus("IN_PROGRESS");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Order result = orderService.updateOrderStatus(TEST_ORDER_ID, TEST_PUBLISHER_ID, "DISPUTED", "服务不符合要求");

            // Then
            assertEquals("DISPUTED", result.getStatus());
            assertEquals("服务不符合要求", result.getLatestRequesterNote());
        }

        @Test
        @DisplayName("DISPUTED -> COMPLETED 争议解决后完成")
        void testUpdateStatusDisputedToCompleted() {
            // Given
            testOrder.setStatus("DISPUTED");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(demandService.updateDemandStatus(TEST_DEMAND_ID, "COMPLETED")).thenReturn(true);

            // When
            Order result = orderService.updateOrderStatus(TEST_ORDER_ID, TEST_PUBLISHER_ID, "COMPLETED", "争议已解决");

            // Then
            assertEquals("COMPLETED", result.getStatus());
            assertNotNull(result.getCompletedAt());
        }

        @Test
        @DisplayName("DISPUTED -> CANCELLED 争议后取消")
        void testUpdateStatusDisputedToCancelled() {
            // Given
            testOrder.setStatus("DISPUTED");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(demandService.updateDemandStatus(TEST_DEMAND_ID, "PENDING")).thenReturn(true);

            // When
            Order result = orderService.updateOrderStatus(TEST_ORDER_ID, TEST_PUBLISHER_ID, "CANCELLED", "争议导致取消");

            // Then
            assertEquals("CANCELLED", result.getStatus());
        }


        @Test
        @DisplayName("非订单相关方无权更新状态")
        void testUpdateStatusUnauthorizedUser() {
            // Given
            Long unauthorizedUserId = 9999L;
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.updateOrderStatus(TEST_ORDER_ID, unauthorizedUserId, "IN_PROGRESS", null));
            assertEquals("无权操作此订单", exception.getMessage());
        }

        @Test
        @DisplayName("COMPLETED状态无法再转换")
        void testUpdateStatusFromCompleted() {
            // Given
            testOrder.setStatus("COMPLETED");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.updateOrderStatus(TEST_ORDER_ID, TEST_PUBLISHER_ID, "IN_PROGRESS", null));
            assertEquals("状态转换不合法：从 COMPLETED 到 IN_PROGRESS", exception.getMessage());
        }

        @Test
        @DisplayName("ACCEPTED不能直接转到COMPLETED")
        void testUpdateStatusAcceptedToCompletedInvalid() {
            // Given
            testOrder.setStatus("ACCEPTED");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.updateOrderStatus(TEST_ORDER_ID, TEST_PUBLISHER_ID, "COMPLETED", null));
            assertEquals("状态转换不合法：从 ACCEPTED 到 COMPLETED", exception.getMessage());
        }

        @Test
        @DisplayName("更新状态时附带备注信息")
        void testUpdateStatusWithNote() {
            // Given
            String note = "请尽快处理";
            testOrder.setStatus("ACCEPTED");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(demandService.updateDemandStatus(TEST_DEMAND_ID, "ACCEPTED")).thenReturn(true);

            // When
            Order result = orderService.updateOrderStatus(TEST_ORDER_ID, TEST_PUBLISHER_ID, "IN_PROGRESS", note);

            // Then
            assertEquals(note, result.getLatestRequesterNote());
        }
    }

    // ==================== 取消订单测试 ====================

    @Nested
    @DisplayName("取消订单测试")
    class CancelOrderTests {

        @Test
        @DisplayName("发布者取消ACCEPTED状态的订单")
        void testCancelOrderByPublisherWhenAccepted() {
            // Given
            testOrder.setStatus("ACCEPTED");
            String reason = "不需要了";
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(demandService.resetStatus(TEST_DEMAND_ID, "PENDING")).thenReturn(true);
            when(demandService.updateOrderId(TEST_DEMAND_ID, null)).thenReturn(true);

            // When
            Order result = orderService.cancelOrder(TEST_ORDER_ID, TEST_PUBLISHER_ID, reason);

            // Then
            assertEquals("CANCELLED", result.getStatus());
            assertEquals(reason, result.getLatestRequesterNote());
            verify(demandService).resetStatus(TEST_DEMAND_ID, "PENDING");
            verify(demandService).updateOrderId(TEST_DEMAND_ID, null);
        }

        @Test
        @DisplayName("接单者取消IN_PROGRESS状态的订单")
        void testCancelOrderByAcceptorWhenInProgress() {
            // Given
            testOrder.setStatus("IN_PROGRESS");
            String reason = "无法完成";
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(demandService.resetStatus(TEST_DEMAND_ID, "PENDING")).thenReturn(true);
            when(demandService.updateOrderId(TEST_DEMAND_ID, null)).thenReturn(true);

            // When
            Order result = orderService.cancelOrder(TEST_ORDER_ID, TEST_ACCEPTOR_ID, reason);

            // Then
            assertEquals("CANCELLED", result.getStatus());
        }

        @Test
        @DisplayName("COMPLETED状态的订单无法取消")
        void testCancelOrderWhenCompleted() {
            // Given
            testOrder.setStatus("COMPLETED");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.cancelOrder(TEST_ORDER_ID, TEST_PUBLISHER_ID, "想取消"));
            assertEquals("当前状态为 COMPLETED，无法取消订单", exception.getMessage());
        }

        @Test
        @DisplayName("非订单相关方不能取消订单")
        void testCancelOrderUnauthorized() {
            // Given
            Long unauthorizedUserId = 9999L;
            testOrder.setStatus("ACCEPTED");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.cancelOrder(TEST_ORDER_ID, unauthorizedUserId, "无权取消"));
            assertEquals("无权操作此订单", exception.getMessage());
        }
    }

    // ==================== 完成订单测试 ====================

    @Nested
    @DisplayName("完成订单测试")
    class CompleteOrderTests {

        @Test
        @DisplayName("发布者完成订单并关联评价")
        void testCompleteOrderWithComment() {
            // Given
            testOrder.setStatus("IN_PROGRESS");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(demandService.updateDemandStatus(TEST_DEMAND_ID, "COMPLETED")).thenReturn(true);

            // When
            Order result = orderService.completeOrderWithComment(TEST_ORDER_ID, TEST_PUBLISHER_ID, TEST_COMMENT_ID);

            // Then
            assertEquals("COMPLETED", result.getStatus());
            assertEquals(TEST_COMMENT_ID, result.getCommentId());
            assertNotNull(result.getCompletedAt());
        }

        @Test
        @DisplayName("接单者不能完成订单（只有发布者可以）")
        void testCompleteOrderByAcceptorFails() {
            // Given
            testOrder.setStatus("IN_PROGRESS");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.completeOrderWithComment(TEST_ORDER_ID, TEST_ACCEPTOR_ID, TEST_COMMENT_ID));
            assertEquals("只有需求发布者可以完成订单", exception.getMessage());
        }

        @Test
        @DisplayName("只有IN_PROGRESS状态的订单可以完成")
        void testCompleteOrderWrongStatus() {
            // Given
            testOrder.setStatus("ACCEPTED");
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.completeOrderWithComment(TEST_ORDER_ID, TEST_PUBLISHER_ID, TEST_COMMENT_ID));
            assertEquals("只有进行中的订单可以完成", exception.getMessage());
        }
    }

    // ==================== 订单备注更新测试 ====================

    @Nested
    @DisplayName("订单备注更新测试")
    class UpdateOrderNoteTests {

        @Test
        @DisplayName("发布者更新订单备注")
        void testUpdateOrderNoteByPublisher() {
            // Given
            String newNote = "请附带发票";
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Order result = orderService.updateOrderNote(TEST_ORDER_ID, TEST_PUBLISHER_ID, newNote);

            // Then
            assertEquals(newNote, result.getLatestRequesterNote());
            assertNotNull(result.getUpdatedAt());
        }

        @Test
        @DisplayName("接单者更新订单备注")
        void testUpdateOrderNoteByAcceptor() {
            // Given
            String newNote = "预计明天完成";
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Order result = orderService.updateOrderNote(TEST_ORDER_ID, TEST_ACCEPTOR_ID, newNote);

            // Then
            assertEquals(newNote, result.getLatestRequesterNote());
        }

        @Test
        @DisplayName("非订单相关方不能更新备注")
        void testUpdateOrderNoteUnauthorized() {
            // Given
            Long unauthorizedUserId = 9999L;
            when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.updateOrderNote(TEST_ORDER_ID, unauthorizedUserId, "无权操作"));
            assertEquals("无权操作此订单", exception.getMessage());
        }
    }

    // ==================== 分页查询测试 ====================

    @Nested
    @DisplayName("分页查询测试")
    class PaginationTests {

        @Test
        @DisplayName("获取发布者的订单分页")
        void testGetOrdersAsPublisher() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(testOrder), pageable, 1);
            when(orderRepository.findByPublisherId(TEST_PUBLISHER_ID, pageable)).thenReturn(page);

            // When
            Page<Order> result = orderService.getOrdersAsPublisher(TEST_PUBLISHER_ID, pageable);

            // Then
            assertEquals(1, result.getTotalElements());
            assertEquals(TEST_ORDER_ID, result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("获取接单者的订单分页")
        void testGetOrdersAsAcceptor() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(testOrder), pageable, 1);
            when(orderRepository.findByAcceptorId(TEST_ACCEPTOR_ID, pageable)).thenReturn(page);

            // When
            Page<Order> result = orderService.getOrdersAsAcceptor(TEST_ACCEPTOR_ID, pageable);

            // Then
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("获取用户所有相关订单分页")
        void testGetAllUserOrders() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(testOrder), pageable, 1);
            when(orderRepository.findAllByUserId(TEST_PUBLISHER_ID, pageable)).thenReturn(page);

            // When
            Page<Order> result = orderService.getAllUserOrders(TEST_PUBLISHER_ID, pageable);

            // Then
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("按状态过滤用户订单")
        void testGetAllUserOrdersWithStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(testOrder), pageable, 1);
            when(orderRepository.findAllByUserIdAndStatus(TEST_PUBLISHER_ID, "ACCEPTED", pageable))
                .thenReturn(page);

            // When
            Page<Order> result = orderService.getAllUserOrdersWithStatus(TEST_PUBLISHER_ID, "ACCEPTED", pageable);

            // Then
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("空状态时返回所有用户订单")
        void testGetAllUserOrdersWithNullStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(testOrder), pageable, 1);
            when(orderRepository.findAllByUserId(TEST_PUBLISHER_ID, pageable)).thenReturn(page);

            // When
            Page<Order> result = orderService.getAllUserOrdersWithStatus(TEST_PUBLISHER_ID, null, pageable);

            // Then
            assertEquals(1, result.getTotalElements());
        }
    }

    // ==================== 搜索订单测试 ====================

    @Nested
    @DisplayName("搜索订单测试")
    class SearchOrdersTests {

        @Test
        @DisplayName("多条件组合搜索订单")
        void testSearchOrdersWithMultipleConditions() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(testOrder), pageable, 1);
            when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Order> result = orderService.searchOrders(
                TEST_DEMAND_ID, TEST_PUBLISHER_ID, TEST_ACCEPTOR_ID, null,
                "ACCEPTED", "测试", pageable
            );

            // Then
            assertNotNull(result);
            verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("按用户ID搜索所有相关订单")
        void testSearchOrdersByUserId() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(testOrder), pageable, 1);
            when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Order> result = orderService.searchOrders(
                null, null, null, TEST_PUBLISHER_ID,
                null, null, pageable
            );

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("只按状态搜索订单")
        void testSearchOrdersByStatusOnly() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(testOrder), pageable, 1);
            when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Order> result = orderService.searchOrders(
                null, null, null, null,
                "ACCEPTED", null, pageable
            );

            // Then
            assertNotNull(result);
        }
    }

    // ==================== 统计信息测试 ====================

    @Nested
    @DisplayName("统计信息测试")
    class StatisticsTests {

        @Test
        @DisplayName("获取用户订单统计信息")
        void testGetOrderStatistics() {
            // Given
            when(orderRepository.countByPublisherId(TEST_PUBLISHER_ID)).thenReturn(5L);
            when(orderRepository.countByAcceptorId(TEST_PUBLISHER_ID)).thenReturn(3L);

            // When
            OrderService.OrderStatistics stats = orderService.getOrderStatistics(TEST_PUBLISHER_ID);

            // Then
            assertEquals(TEST_PUBLISHER_ID, stats.getUserId());
            assertEquals(5L, stats.getTotalAsPublisher());
            assertEquals(3L, stats.getTotalAsAcceptor());
            assertEquals(8L, stats.getTotalOrders());
        }
    }

    // ==================== 权限验证测试 ====================

    @Nested
    @DisplayName("权限验证测试")
    class PermissionTests {

        @Test
        @DisplayName("检查用户是否与订单相关")
        void testIsUserRelatedToOrder() {
            // Given
            when(orderRepository.isUserRelatedToOrder(TEST_ORDER_ID, TEST_PUBLISHER_ID)).thenReturn(true);
            when(orderRepository.isUserRelatedToOrder(TEST_ORDER_ID, TEST_ACCEPTOR_ID)).thenReturn(true);
            when(orderRepository.isUserRelatedToOrder(TEST_ORDER_ID, 9999L)).thenReturn(false);

            // Then
            assertTrue(orderService.isUserRelatedToOrder(TEST_ORDER_ID, TEST_PUBLISHER_ID));
            assertTrue(orderService.isUserRelatedToOrder(TEST_ORDER_ID, TEST_ACCEPTOR_ID));
            assertFalse(orderService.isUserRelatedToOrder(TEST_ORDER_ID, 9999L));
        }
    }

    // ==================== 边界和异常测试 ====================

    @Nested
    @DisplayName("边界和异常测试")
    class BoundaryAndExceptionTests {

        @Test
        @DisplayName("订单不存在时更新状态抛出异常")
        void testUpdateStatusOrderNotFound() {
            // Given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.updateOrderStatus(999L, TEST_PUBLISHER_ID, "IN_PROGRESS", null));
            assertEquals("订单不存在", exception.getMessage());
        }

        @Test
        @DisplayName("订单不存在时取消订单抛出异常")
        void testCancelOrderNotFound() {
            // Given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.cancelOrder(999L, TEST_PUBLISHER_ID, "原因"));
            assertEquals("订单不存在", exception.getMessage());
        }

        @Test
        @DisplayName("订单不存在时完成订单抛出异常")
        void testCompleteOrderNotFound() {
            // Given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.completeOrderWithComment(999L, TEST_PUBLISHER_ID, TEST_COMMENT_ID));
            assertEquals("订单不存在", exception.getMessage());
        }

        @Test
        @DisplayName("订单不存在时更新备注抛出异常")
        void testUpdateNoteOrderNotFound() {
            // Given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.updateOrderNote(999L, TEST_PUBLISHER_ID, "备注"));
            assertEquals("订单不存在", exception.getMessage());
        }
    }
}