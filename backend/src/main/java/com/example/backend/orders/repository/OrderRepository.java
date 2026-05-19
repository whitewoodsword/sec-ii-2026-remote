package com.example.backend.orders.repository;

import com.example.backend.orders.model.DemoUser;
import com.example.backend.orders.model.HelpRequest;
import com.example.backend.orders.model.ServiceOrder;

import java.util.Collection;
import java.util.Optional;

public interface OrderRepository {
    Collection<DemoUser> findAllUsers();

    Optional<DemoUser> findUserById(long userId);

    Collection<HelpRequest> findAllRequests();

    Optional<HelpRequest> findRequestById(long requestId);

    Collection<ServiceOrder> findAllOrders();

    Optional<ServiceOrder> findOrderById(long orderId);

    HelpRequest storeRequest(HelpRequest request);

    ServiceOrder storeOrder(ServiceOrder order);
}
