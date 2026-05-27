package com.example.backend.orders.repository;

import com.example.backend.entity.Demand;
import com.example.backend.entity.DemandApplication;
import com.example.backend.entity.Order;
import com.example.backend.entity.OrderTimelineRecord;
import com.example.backend.entity.User;
import com.example.backend.orders.model.ApplicationStatus;
import com.example.backend.orders.model.DemoUser;
import com.example.backend.orders.model.HelpRequest;
import com.example.backend.orders.model.HelpRequestStatus;
import com.example.backend.orders.model.OrderStatus;
import com.example.backend.orders.model.OrderTimelineEntry;
import com.example.backend.orders.model.RequestApplication;
import com.example.backend.orders.model.ServiceOrder;
import com.example.backend.repository.DemandApplicationRepository;
import com.example.backend.repository.DemandRepository;
import com.example.backend.repository.OrderEntityRepository;
import com.example.backend.repository.OrderTimelineRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class JpaOrderRepository implements OrderRepository {
    private static final String DEFAULT_ACADEMY = "Campus Member";

    private final UserRepository userRepository;
    private final DemandRepository demandRepository;
    private final DemandApplicationRepository demandApplicationRepository;
    private final OrderEntityRepository orderEntityRepository;
    private final OrderTimelineRepository orderTimelineRepository;

    public JpaOrderRepository(
            UserRepository userRepository,
            DemandRepository demandRepository,
            DemandApplicationRepository demandApplicationRepository,
            OrderEntityRepository orderEntityRepository,
            OrderTimelineRepository orderTimelineRepository
    ) {
        this.userRepository = userRepository;
        this.demandRepository = demandRepository;
        this.demandApplicationRepository = demandApplicationRepository;
        this.orderEntityRepository = orderEntityRepository;
        this.orderTimelineRepository = orderTimelineRepository;
    }

    @Override
    public Collection<DemoUser> findAllUsers() {
        List<User> users = userRepository.findAll().stream()
                .filter(user -> !user.isSuperAdmin())
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .toList();
        Set<Long> requesterIds = demandRepository.findAll().stream()
                .map(Demand::getPublisherId)
                .collect(Collectors.toSet());
        Set<Long> providerIds = orderEntityRepository.findAll().stream()
                .map(Order::getAcceptorId)
                .collect(Collectors.toSet());

        return users.stream()
                .map(user -> toDemoUser(user, requesterIds, providerIds))
                .toList();
    }

    @Override
    public Optional<DemoUser> findUserById(long userId) {
        Set<Long> requesterIds = demandRepository.findAll().stream()
                .map(Demand::getPublisherId)
                .collect(Collectors.toSet());
        Set<Long> providerIds = orderEntityRepository.findAll().stream()
                .map(Order::getAcceptorId)
                .collect(Collectors.toSet());

        return userRepository.findById(userId)
                .map(user -> toDemoUser(user, requesterIds, providerIds));
    }

    @Override
    public Collection<HelpRequest> findAllRequests() {
        return demandRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toHelpRequest)
                .toList();
    }

    @Override
    public Optional<HelpRequest> findRequestById(long requestId) {
        return demandRepository.findById(requestId)
                .map(this::toHelpRequest);
    }

    @Override
    public Collection<ServiceOrder> findAllOrders() {
        return orderEntityRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toServiceOrder)
                .toList();
    }

    @Override
    public Optional<ServiceOrder> findOrderById(long orderId) {
        return orderEntityRepository.findById(orderId)
                .map(this::toServiceOrder);
    }

    @Override
    @Transactional
    public HelpRequest storeRequest(HelpRequest request) {
        Demand demand = demandRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown request id: " + request.getId()));

        demand.setStatus(toDemandStatus(request.getStatus()));
        demand.setOrderId(request.getCurrentOrderId());
        demand.setUpdatedAt(LocalDateTime.now());
        demandRepository.save(demand);

        Map<Long, DemandApplication> existingApplications = demandApplicationRepository
                .findByDemandIdOrderByCreatedAtAsc(demand.getId())
                .stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(DemandApplication::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        for (RequestApplication application : request.getApplications()) {
            DemandApplication entity = application.getId() > 0
                    ? existingApplications.remove(application.getId())
                    : new DemandApplication();
            entity.setDemandId(demand.getId());
            entity.setApplicantUserId(application.getApplicantUserId());
            entity.setMessage(application.getMessage());
            entity.setStatus(application.getStatus().name());
            entity.setCreatedAt(application.getCreatedAt());
            entity.setUpdatedAt(LocalDateTime.now());
            demandApplicationRepository.save(entity);
        }

        if (!existingApplications.isEmpty()) {
            demandApplicationRepository.deleteAll(existingApplications.values());
        }

        return findRequestById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Unable to reload request: " + request.getId()));
    }

    @Override
    @Transactional
    public ServiceOrder storeOrder(ServiceOrder order) {
        Order entity = order.getId() > 0
                ? orderEntityRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown order id: " + order.getId()))
                : new Order();

        entity.setDemandId(order.getRequestId());
        entity.setPublisherId(order.getRequesterUserId());
        entity.setAcceptorId(order.getProviderUserId());
        entity.setStatus(order.getStatus().name());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        entity.setCompletedAt(order.getCompletedAt());
        entity.setLatestRequesterNote(order.getLatestRequesterNote());

        Order savedOrder = orderEntityRepository.save(entity);

        orderTimelineRepository.deleteByOrderId(savedOrder.getId());
        for (OrderTimelineEntry entry : order.getTimelineEntries()) {
            OrderTimelineRecord timelineRecord = new OrderTimelineRecord();
            timelineRecord.setOrderId(savedOrder.getId());
            timelineRecord.setActorName(entry.actorName());
            timelineRecord.setDescription(entry.description());
            timelineRecord.setHappenedAt(entry.happenedAt());
            orderTimelineRepository.save(timelineRecord);
        }

        return findOrderById(savedOrder.getId())
                .orElseThrow(() -> new IllegalArgumentException("Unable to reload order: " + savedOrder.getId()));
    }

    private DemoUser toDemoUser(User user, Set<Long> requesterIds, Set<Long> providerIds) {
        return new DemoUser(
                user.getId(),
                user.getName(),
                DEFAULT_ACADEMY,
                resolveRoleLabel(user, requesterIds, providerIds)
        );
    }

    private String resolveRoleLabel(User user, Set<Long> requesterIds, Set<Long> providerIds) {
        boolean requester = requesterIds.contains(user.getId());
        boolean provider = providerIds.contains(user.getId());
        if (requester && provider) {
            return "Requester / Provider";
        }
        if (requester) {
            return "Requester";
        }
        if (provider) {
            return "Provider";
        }
        if (user.isAdmin()) {
            return "Admin";
        }
        return "Student";
    }

    private HelpRequest toHelpRequest(Demand demand) {
        HelpRequest request = new HelpRequest(
                demand.getId(),
                demand.getPublisherId(),
                demand.getTitle(),
                demand.getDescription(),
                demand.getCategory(),
                demand.getLocation(),
                formatReward(demand.getReward()),
                demand.getDeadline(),
                demand.getCreatedAt()
        );
        request.restoreState(toHelpRequestStatus(demand.getStatus()), demand.getOrderId());

        demandApplicationRepository.findByDemandIdOrderByCreatedAtAsc(demand.getId())
                .forEach(applicationEntity -> request.addApplication(toRequestApplication(applicationEntity)));
        return request;
    }

    private ServiceOrder toServiceOrder(Order order) {
        ServiceOrder serviceOrder = new ServiceOrder(
                order.getId(),
                order.getDemandId(),
                order.getPublisherId(),
                order.getAcceptorId(),
                order.getCreatedAt()
        );
        serviceOrder.restoreState(
                toOrderStatus(order.getStatus()),
                order.getUpdatedAt(),
                order.getCompletedAt(),
                order.getLatestRequesterNote()
        );
        orderTimelineRepository.findByOrderIdOrderByHappenedAtAsc(order.getId())
                .forEach(timelineRecord -> serviceOrder.addTimelineEntry(new OrderTimelineEntry(
                        timelineRecord.getHappenedAt(),
                        timelineRecord.getActorName(),
                        timelineRecord.getDescription()
                )));
        return serviceOrder;
    }

    private RequestApplication toRequestApplication(DemandApplication entity) {
        RequestApplication application = new RequestApplication(
                entity.getId(),
                entity.getDemandId(),
                entity.getApplicantUserId(),
                entity.getMessage(),
                entity.getCreatedAt()
        );
        application.restoreStatus(toApplicationStatus(entity.getStatus()));
        return application;
    }

    private HelpRequestStatus toHelpRequestStatus(String status) {
        if (status == null) {
            return HelpRequestStatus.OPEN;
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "ACCEPTED" -> HelpRequestStatus.ORDER_CREATED;
            case "COMPLETED" -> HelpRequestStatus.COMPLETED;
            case "CANCELLED", "CLOSED" -> HelpRequestStatus.CLOSED;
            default -> HelpRequestStatus.OPEN;
        };
    }

    private String toDemandStatus(HelpRequestStatus status) {
        return switch (status) {
            case OPEN -> "PENDING";
            case ORDER_CREATED -> "ACCEPTED";
            case COMPLETED -> "COMPLETED";
            case CLOSED -> "CANCELLED";
        };
    }

    private OrderStatus toOrderStatus(String status) {
        if (status == null) {
            return OrderStatus.PENDING_ACCEPTANCE;
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "IN_PROGRESS" -> OrderStatus.IN_PROGRESS;
            case "PENDING_CONFIRMATION" -> OrderStatus.PENDING_CONFIRMATION;
            case "COMPLETED" -> OrderStatus.COMPLETED;
            default -> OrderStatus.PENDING_ACCEPTANCE;
        };
    }

    private ApplicationStatus toApplicationStatus(String status) {
        if (status == null) {
            return ApplicationStatus.PENDING;
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "SELECTED" -> ApplicationStatus.SELECTED;
            case "REJECTED" -> ApplicationStatus.REJECTED;
            default -> ApplicationStatus.PENDING;
        };
    }

    private String formatReward(Double reward) {
        if (reward == null) {
            return "Negotiable";
        }
        return String.format(Locale.US, "CNY %.2f", reward);
    }
}
