package com.example.backend.orders.api;

import com.example.backend.orders.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/users")
    public List<OrderService.UserView> listUsers() {
        return orderService.listUsers();
    }

    @GetMapping("/requests")
    public List<OrderService.RequestCardView> listRequests(
            @RequestParam(required = false) Long viewerId,
            @RequestParam(required = false) String category
    ) {
        return orderService.listRequests(viewerId, category);
    }

    @GetMapping("/requests/{requestId}")
    public OrderService.RequestDetailView getRequestDetail(
            @PathVariable long requestId,
            @RequestParam(required = false) Long viewerId
    ) {
        return orderService.getRequestDetail(requestId, viewerId);
    }

    @PostMapping("/requests/{requestId}/applications")
    public OrderService.RequestDetailView apply(
            @PathVariable long requestId,
            @RequestBody ApplyCommand command
    ) {
        return orderService.apply(requestId, command.applicantId(), command.message());
    }

    @PostMapping("/requests/{requestId}/applications/{applicationId}/select")
    public OrderService.RequestDetailView selectApplication(
            @PathVariable long requestId,
            @PathVariable long applicationId,
            @RequestBody SelectCommand command
    ) {
        return orderService.selectApplication(requestId, applicationId, command.requesterId());
    }

    @GetMapping
    public List<OrderService.OrderSummaryView> listOrders(@RequestParam long userId) {
        return orderService.listOrders(userId);
    }

    @GetMapping("/{orderId}")
    public OrderService.OrderDetailView getOrderDetail(
            @PathVariable long orderId,
            @RequestParam long viewerId
    ) {
        return orderService.getOrderDetail(orderId, viewerId);
    }

    @PostMapping("/{orderId}/actions")
    public OrderService.OrderDetailView actOnOrder(
            @PathVariable long orderId,
            @RequestBody OrderActionCommand command
    ) {
        return orderService.performAction(orderId, command.actorId(), command.action(), command.note());
    }

    public record ApplyCommand(long applicantId, String message) {
    }

    public record SelectCommand(long requesterId) {
    }

    public record OrderActionCommand(long actorId, String action, String note) {
    }
}
