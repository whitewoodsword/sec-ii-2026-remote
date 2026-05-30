package com.example.backend.service;

import com.example.backend.entity.Demand;
import com.example.backend.entity.Order;
import com.example.backend.entity.User;
import com.example.backend.repository.DemandRepository;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemandService demandService;

    // 有效的订单状态
    private static final Set<String> VALID_STATUSES = Set.of(
        "ACCEPTED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "DISPUTED"
    );

    // 可转换的状态映射
    private static final Set<String> ACCEPTABLE_NEXT_STATUSES_FROM_ACCEPTED = Set.of("IN_PROGRESS", "CANCELLED");
    private static final Set<String> ACCEPTABLE_NEXT_STATUSES_FROM_IN_PROGRESS = Set.of("COMPLETED", "DISPUTED");
    private static final Set<String> ACCEPTABLE_NEXT_STATUSES_FROM_COMPLETED = Set.of();
    private static final Set<String> ACCEPTABLE_NEXT_STATUSES_FROM_CANCELLED = Set.of();
    private static final Set<String> ACCEPTABLE_NEXT_STATUSES_FROM_DISPUTED = Set.of("COMPLETED", "CANCELLED");

    /**
     * 创建订单（用户接单）
     * @param demandId 需求ID
     * @param acceptorId 接单者ID
     * @return 创建的订单
     */
    @Transactional
    public Order createOrder(Long demandId, Long acceptorId) {
        // 检查需求是否存在
        Optional<Demand> demandOpt = demandService.getDemandById(demandId);
        if (demandOpt.isEmpty()) {
            throw new RuntimeException("需求不存在");
        }

        Demand demand = demandOpt.get();
        
        // 检查需求状态是否为PENDING
        if (!"PENDING".equals(demand.getStatus())) {
            throw new RuntimeException("该需求当前状态为" + demand.getStatus() + "，无法接单");
        }

        // 检查发布者不能接自己的单
        if (demand.getPublisherId().equals(acceptorId)) {
            throw new RuntimeException("不能接自己发布的需求");
        }

        // 检查是否已有订单
        Optional<Order> existingOrder = orderRepository.findByDemandId(demandId);
        if (existingOrder.isPresent()) {
            throw new RuntimeException("该需求已被接单");
        }

        // 检查接单者是否存在
        Optional<User> acceptorOpt = userRepository.findById(acceptorId);
        if (acceptorOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        // 创建订单
        Order order = new Order(demandId, demand.getPublisherId(), acceptorId);
        order.setStatus("ACCEPTED");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        
        // 更新需求状态为ACCEPTED，并绑定订单ID
        demandService.updateDemandStatus(demandId, "ACCEPTED");
        demandService.updateOrderId(demandId, savedOrder.getId());
        
        return savedOrder;
    }

    /**
     * 根据ID获取订单
     */
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * 根据需求ID获取订单
     */
    public Optional<Order> getOrderByDemandId(Long demandId) {
        return orderRepository.findByDemandId(demandId);
    }

    /**
     * 更新订单状态
     * @param orderId 订单ID
     * @param userId 操作用户ID（必须是订单相关方）
     * @param newStatus 新状态
     * @param note 备注信息（可选）
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, Long userId, String newStatus, String note) {
        // 验证状态值
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new RuntimeException("无效的状态值，有效值为：ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED, DISPUTED");
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }

        Order order = orderOpt.get();
        
        // 验证用户权限（必须是订单相关方）
        if (!order.getPublisherId().equals(userId) && !order.getAcceptorId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        // 验证状态转换是否合法
        if (!isValidStatusTransition(order.getStatus(), newStatus, userId, order)) {
            throw new RuntimeException("状态转换不合法：从 " + order.getStatus() + " 到 " + newStatus);
        }

        // 更新状态
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        
        if (note != null && !note.isEmpty()) {
            order.setLatestRequesterNote(note);
        }
        
        // 如果是完成状态，设置完成时间
        if ("COMPLETED".equals(newStatus)) {
            order.setCompletedAt(LocalDateTime.now());
        }
        
        // 同步更新需求状态
        updateDemandStatusFromOrder(order.getDemandId(), newStatus);
        
        return orderRepository.save(order);
    }

    /**
     * 更新订单备注
     */
    @Transactional
    public Order updateOrderNote(Long orderId, Long userId, String note) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }

        Order order = orderOpt.get();
        
        // 验证用户权限
        if (!order.getPublisherId().equals(userId) && !order.getAcceptorId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        order.setLatestRequesterNote(note);
        order.setUpdatedAt(LocalDateTime.now());
        
        return orderRepository.save(order);
    }

    /**
     * 取消订单（特殊处理，允许PENDING状态的需求被取消）
     */
    @Transactional
    public Order cancelOrder(Long orderId, Long userId, String reason) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }

        Order order = orderOpt.get();
        
        // 只有发布者或接单者可以取消订单
        if (!order.getPublisherId().equals(userId) && !order.getAcceptorId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        String currentStatus = order.getStatus();
        if (!"ACCEPTED".equals(currentStatus) && !"IN_PROGRESS".equals(currentStatus)) {
            throw new RuntimeException("当前状态为 " + currentStatus + "，无法取消订单");
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        if (reason != null) {
            order.setLatestRequesterNote(reason);
        }
        
        // 恢复需求状态为PENDING
        demandService.resetStatus(order.getDemandId(), "PENDING");
        // 清除需求上的订单ID
        demandService.updateOrderId(order.getDemandId(), null);
        
        return orderRepository.save(order);
    }

    /**
     * 完成订单（带评价关联）
     */
    @Transactional
    public Order completeOrderWithComment(Long orderId, Long userId, Long commentId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }

        Order order = orderOpt.get();
        
        // 只有发布者可以完成订单
        if (!order.getPublisherId().equals(userId)) {
            throw new RuntimeException("只有需求发布者可以完成订单");
        }

        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new RuntimeException("只有进行中的订单可以完成");
        }

        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setCommentId(commentId);
        
        // 更新需求状态
        demandService.updateDemandStatus(order.getDemandId(), "COMPLETED");
        
        return orderRepository.save(order);
    }

    /**
     * 获取用户作为发布者的订单（分页）
     */
    public Page<Order> getOrdersAsPublisher(Long publisherId, Pageable pageable) {
        return orderRepository.findByPublisherId(publisherId, pageable);
    }

    /**
     * 获取用户作为接单者的订单（分页）
     */
    public Page<Order> getOrdersAsAcceptor(Long acceptorId, Pageable pageable) {
        return orderRepository.findByAcceptorId(acceptorId, pageable);
    }

    /**
     * 获取用户所有相关订单（分页）
     */
    public Page<Order> getAllUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findAllByUserId(userId, pageable);
    }

    /**
     * 多条件查询订单
     */
    public Page<Order> searchOrders(Long demandId, Long publisherId, Long acceptorId, 
                                     String status, String keyword, Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (demandId != null) {
                predicates.add(cb.equal(root.get("demandId"), demandId));
            }
            if (publisherId != null) {
                predicates.add(cb.equal(root.get("publisherId"), publisherId));
            }
            if (acceptorId != null) {
                predicates.add(cb.equal(root.get("acceptorId"), acceptorId));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isEmpty()) {
                Predicate noteLike = cb.like(root.get("latestRequesterNote"), "%" + keyword + "%");
                predicates.add(noteLike);
            }
            
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return orderRepository.findAll(spec, pageable);
    }

    /**
     * 获取订单统计信息
     */
    public OrderStatistics getOrderStatistics(Long userId) {
        OrderStatistics stats = new OrderStatistics();
        stats.setUserId(userId);
        stats.setTotalAsPublisher(orderRepository.countByPublisherId(userId));
        stats.setTotalAsAcceptor(orderRepository.countByAcceptorId(userId));
        
        return stats;
    }

    /**
     * 验证状态转换是否合法
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus, Long userId, Order order) {
        return switch (currentStatus) {
            case "ACCEPTED" -> ACCEPTABLE_NEXT_STATUSES_FROM_ACCEPTED.contains(newStatus);
            case "IN_PROGRESS" -> ACCEPTABLE_NEXT_STATUSES_FROM_IN_PROGRESS.contains(newStatus);
            case "COMPLETED" -> ACCEPTABLE_NEXT_STATUSES_FROM_COMPLETED.contains(newStatus);
            case "CANCELLED" -> ACCEPTABLE_NEXT_STATUSES_FROM_CANCELLED.contains(newStatus);
            case "DISPUTED" -> ACCEPTABLE_NEXT_STATUSES_FROM_DISPUTED.contains(newStatus);
            default -> false;
        };
    }

    /**
     * 根据订单状态同步更新需求状态
     */
    private void updateDemandStatusFromOrder(Long demandId, String orderStatus) {
        String demandStatus = switch (orderStatus) {
            case "ACCEPTED", "IN_PROGRESS" -> "ACCEPTED";
            case "COMPLETED" -> "COMPLETED";
            case "CANCELLED" -> "PENDING";
            default -> null;
        };
        
        if (demandStatus != null) {
            demandService.updateDemandStatus(demandId, demandStatus);
        }
    }

    /**
     * 检查用户是否与订单相关
     */
    public boolean isUserRelatedToOrder(Long orderId, Long userId) {
        return orderRepository.isUserRelatedToOrder(orderId, userId);
    }

    // ========== 内部类 ==========

    /**
     * 订单统计信息
     */
    public static class OrderStatistics {
        private Long userId;
        private long totalAsPublisher;
        private long totalAsAcceptor;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public long getTotalAsPublisher() { return totalAsPublisher; }
        public void setTotalAsPublisher(long totalAsPublisher) { this.totalAsPublisher = totalAsPublisher; }
        public long getTotalAsAcceptor() { return totalAsAcceptor; }
        public void setTotalAsAcceptor(long totalAsAcceptor) { this.totalAsAcceptor = totalAsAcceptor; }
        public long getTotalOrders() { return totalAsPublisher + totalAsAcceptor; }
    }
}