package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.entity.Order;
import com.example.backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单（用户接单）
     * POST /orders/create?demandId={demandId}&userId={acceptorId}
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Order>> createOrder(
            @RequestParam Long demandId,
            @RequestParam Long userId) {
        
        try {
            Order order = orderService.createOrder(demandId, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 获取订单详情
     * GET /orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(@PathVariable Long id) {
        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(orderOpt.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "订单不存在"));
        }
    }

    /**
     * 根据需求ID获取订单
     * GET /orders/demand/{demandId}
     */
    @GetMapping("/demand/{demandId}")
    public ResponseEntity<ApiResponse<Order>> getOrderByDemandId(@PathVariable Long demandId) {
        Optional<Order> orderOpt = orderService.getOrderByDemandId(demandId);
        if (orderOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(orderOpt.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "该需求暂无订单"));
        }
    }

    /**
     * 更新订单状态
     * PATCH /orders/{id}/status?userId={userId}&status={status}&note={note}
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam String status,
            @RequestParam(required = false) String note) {
        
        try {
            Order updatedOrder = orderService.updateOrderStatus(id, userId, status, note);
            return ResponseEntity.ok(ApiResponse.success(updatedOrder));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 更新订单备注
     * PATCH /orders/{id}/note?userId={userId}&note={note}
     */
    @PatchMapping("/{id}/note")
    public ResponseEntity<ApiResponse<Order>> updateOrderNote(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam String note) {
        
        try {
            Order updatedOrder = orderService.updateOrderNote(id, userId, note);
            return ResponseEntity.ok(ApiResponse.success(updatedOrder));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 取消订单
     * POST /orders/{id}/cancel?userId={userId}&reason={reason}
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Order>> cancelOrder(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(required = false) String reason) {
        
        try {
            Order cancelledOrder = orderService.cancelOrder(id, userId, reason);
            return ResponseEntity.ok(ApiResponse.success(cancelledOrder));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 完成订单并关联评价
     * POST /orders/{id}/complete?userId={userId}&commentId={commentId}
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<Order>> completeOrderWithComment(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam Long commentId) {
        
        try {
            Order completedOrder = orderService.completeOrderWithComment(id, userId, commentId);
            return ResponseEntity.ok(ApiResponse.success(completedOrder));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 获取用户作为发布者的订单（分页）
     * GET /orders/publisher/{publisherId}?page=0&size=10&status=可选
     */
    @GetMapping("/publisher/{publisherId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrdersAsPublisher(
            @PathVariable Long publisherId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Order> orderPage;
        if (status != null && !status.isEmpty()) {
            // 这里需要在Service中添加对应方法，或者使用通用搜索
            orderPage = orderService.searchOrders(null, publisherId, null, status, null, pageable);
        } else {
            orderPage = orderService.getOrdersAsPublisher(publisherId, pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", orderPage.getContent());
        response.put("totalElements", orderPage.getTotalElements());
        response.put("totalPages", orderPage.getTotalPages());
        response.put("currentPage", orderPage.getNumber());
        response.put("pageSize", orderPage.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取用户作为接单者的订单（分页）
     * GET /orders/acceptor/{acceptorId}?page=0&size=10&status=可选
     */
    @GetMapping("/acceptor/{acceptorId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrdersAsAcceptor(
            @PathVariable Long acceptorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Order> orderPage;
        if (status != null && !status.isEmpty()) {
            orderPage = orderService.searchOrders(null, null, acceptorId, status, null, pageable);
        } else {
            orderPage = orderService.getOrdersAsAcceptor(acceptorId, pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", orderPage.getContent());
        response.put("totalElements", orderPage.getTotalElements());
        response.put("totalPages", orderPage.getTotalPages());
        response.put("currentPage", orderPage.getNumber());
        response.put("pageSize", orderPage.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取用户所有相关订单（分页）
     * GET /orders/user/{userId}?page=0&size=10&role=all/publisher/acceptor
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Order> orderPage;
        
        if ("publisher".equalsIgnoreCase(role)) {
            if (status != null && !status.isEmpty()) {
                orderPage = orderService.searchOrders(null, userId, null, status, null, pageable);
            } else {
                orderPage = orderService.getOrdersAsPublisher(userId, pageable);
            }
        } else if ("acceptor".equalsIgnoreCase(role)) {
            if (status != null && !status.isEmpty()) {
                orderPage = orderService.searchOrders(null, null, userId, status, null, pageable);
            } else {
                orderPage = orderService.getOrdersAsAcceptor(userId, pageable);
            }
        } else {
            if (status != null && !status.isEmpty()) {
                orderPage = orderService.searchOrders(null, userId, userId, status, null, pageable);
            } else {
                orderPage = orderService.getAllUserOrders(userId, pageable);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", orderPage.getContent());
        response.put("totalElements", orderPage.getTotalElements());
        response.put("totalPages", orderPage.getTotalPages());
        response.put("currentPage", orderPage.getNumber());
        response.put("pageSize", orderPage.getSize());
        response.put("role", role);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 多条件搜索订单
     * GET /orders/search?demandId=&publisherId=&acceptorId=&status=&keyword=&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchOrders(
            @RequestParam(required = false) Long demandId,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) Long acceptorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Order> orderPage = orderService.searchOrders(demandId, publisherId, acceptorId, status, keyword, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", orderPage.getContent());
        response.put("totalElements", orderPage.getTotalElements());
        response.put("totalPages", orderPage.getTotalPages());
        response.put("currentPage", orderPage.getNumber());
        response.put("pageSize", orderPage.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取订单统计信息
     * GET /orders/statistics/{userId}
     */
    @GetMapping("/statistics/{userId}")
    public ResponseEntity<ApiResponse<OrderService.OrderStatistics>> getOrderStatistics(@PathVariable Long userId) {
        OrderService.OrderStatistics stats = orderService.getOrderStatistics(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取有效订单状态列表
     * GET /orders/statuses
     */
    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<String[]>> getValidStatuses() {
        String[] statuses = {"ACCEPTED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "DISPUTED"};
        return ResponseEntity.ok(ApiResponse.success(statuses));
    }
}