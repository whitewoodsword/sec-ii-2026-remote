package com.example.backend.orders.service;

import com.example.backend.orders.model.ApplicationStatus;
import com.example.backend.orders.model.DemoUser;
import com.example.backend.orders.model.HelpRequest;
import com.example.backend.orders.model.HelpRequestStatus;
import com.example.backend.orders.model.OrderAction;
import com.example.backend.orders.model.OrderStatus;
import com.example.backend.orders.model.OrderTimelineEntry;
import com.example.backend.orders.model.RequestApplication;
import com.example.backend.orders.model.ServiceOrder;
import com.example.backend.orders.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UserView> listUsers() {
        return repository.findAllUsers().stream()
                .sorted(Comparator.comparingLong(DemoUser::id))
                .map(user -> new UserView(user.id(), user.name(), user.academy(), user.roleLabel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public synchronized List<RequestCardView> listRequests(Long viewerId, String category) {
        return repository.findAllRequests().stream()
                .filter(request -> shouldShowRequest(request, viewerId))
                .filter(request -> matchesCategory(request, category))
                .sorted(Comparator.comparing(HelpRequest::getCreatedAt).reversed())
                .map(request -> toRequestCard(request, viewerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public synchronized RequestDetailView getRequestDetail(long requestId, Long viewerId) {
        HelpRequest request = requireRequest(requestId);
        return toRequestDetail(request, viewerId);
    }

    @Transactional
    public synchronized RequestDetailView apply(long requestId, long applicantId, String rawMessage) {
        HelpRequest request = requireRequest(requestId);
        DemoUser applicant = requireUser(applicantId);
        String message = normalizeMessage(rawMessage);

        if (request.getRequesterUserId() == applicant.id()) {
            throw new OrderModuleException(HttpStatus.BAD_REQUEST, "You cannot apply to your own request.");
        }
        if (request.getStatus() != HelpRequestStatus.OPEN) {
            throw new OrderModuleException(HttpStatus.BAD_REQUEST, "This request is no longer open for applications.");
        }

        boolean duplicated = request.getApplications().stream()
                .anyMatch(application -> application.getApplicantUserId() == applicant.id()
                        && application.getStatus() != ApplicationStatus.REJECTED);
        if (duplicated) {
            throw new OrderModuleException(HttpStatus.BAD_REQUEST, "You already have an active application for this request.");
        }

        RequestApplication application = new RequestApplication(
                0L,
                request.getId(),
                applicant.id(),
                message,
                LocalDateTime.now()
        );
        request.addApplication(application);
        repository.storeRequest(request);
        return getRequestDetail(requestId, applicantId);
    }

    @Transactional
    public synchronized RequestDetailView selectApplication(long requestId, long applicationId, long requesterId) {
        HelpRequest request = requireRequest(requestId);
        requireUser(requesterId);

        if (request.getRequesterUserId() != requesterId) {
            throw new OrderModuleException(HttpStatus.FORBIDDEN, "Only the requester can select a provider.");
        }
        if (request.getStatus() != HelpRequestStatus.OPEN) {
            throw new OrderModuleException(HttpStatus.BAD_REQUEST, "This request is not in the recruiting stage anymore.");
        }

        RequestApplication selected = request.getApplications().stream()
                .filter(item -> item.getId() == applicationId)
                .findFirst()
                .orElseThrow(() -> new OrderModuleException(HttpStatus.NOT_FOUND, "The selected application does not exist."));

        if (selected.getStatus() != ApplicationStatus.PENDING) {
            throw new OrderModuleException(HttpStatus.BAD_REQUEST, "This application has already been processed.");
        }

        request.getApplications().forEach(item -> {
            if (item.getId() == applicationId) {
                item.select();
            } else if (item.getStatus() == ApplicationStatus.PENDING) {
                item.reject();
            }
        });

        LocalDateTime now = LocalDateTime.now();
        ServiceOrder order = new ServiceOrder(
                0L,
                request.getId(),
                request.getRequesterUserId(),
                selected.getApplicantUserId(),
                now
        );
        order.addTimelineEntry(new OrderTimelineEntry(now, "System", "Requester selected a provider and the order was created."));
        ServiceOrder storedOrder = repository.storeOrder(order);

        request.markOrderCreated(storedOrder.getId());
        repository.storeRequest(request);
        return getRequestDetail(requestId, requesterId);
    }

    @Transactional(readOnly = true)
    public synchronized List<OrderSummaryView> listOrders(long userId) {
        requireUser(userId);
        return repository.findAllOrders().stream()
                .filter(order -> order.getRequesterUserId() == userId || order.getProviderUserId() == userId)
                .sorted(Comparator.comparing(ServiceOrder::getUpdatedAt).reversed())
                .map(order -> toOrderSummary(order, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public synchronized OrderDetailView getOrderDetail(long orderId, long viewerId) {
        ServiceOrder order = requireOrder(orderId);
        requireParticipant(order, viewerId);
        return toOrderDetail(order, viewerId);
    }

    @Transactional
    public synchronized OrderDetailView performAction(long orderId, long actorId, String actionText, String rawNote) {
        ServiceOrder order = requireOrder(orderId);
        DemoUser actor = requireUser(actorId);
        requireParticipant(order, actor.id());

        OrderAction action = OrderAction.fromText(actionText);
        String note = rawNote == null ? "" : rawNote.trim();
        HelpRequest request = requireRequest(order.getRequestId());
        LocalDateTime now = LocalDateTime.now();

        switch (action) {
            case START -> {
                if (order.getProviderUserId() != actor.id()) {
                    throw new OrderModuleException(HttpStatus.FORBIDDEN, "Only the provider can start the task.");
                }
                if (order.getStatus() != OrderStatus.PENDING_ACCEPTANCE) {
                    throw new OrderModuleException(HttpStatus.BAD_REQUEST, "The current order state cannot be started.");
                }
                order.updateStatus(OrderStatus.IN_PROGRESS, now);
                order.addTimelineEntry(new OrderTimelineEntry(now, actor.name(), "Provider started the task."));
            }
            case SUBMIT_COMPLETION -> {
                if (order.getProviderUserId() != actor.id()) {
                    throw new OrderModuleException(HttpStatus.FORBIDDEN, "Only the provider can submit completion.");
                }
                if (order.getStatus() != OrderStatus.IN_PROGRESS) {
                    throw new OrderModuleException(HttpStatus.BAD_REQUEST, "Only in-progress orders can submit completion.");
                }
                order.updateStatus(OrderStatus.PENDING_CONFIRMATION, now);
                order.addTimelineEntry(new OrderTimelineEntry(now, actor.name(), "Provider submitted completion and is waiting for confirmation."));
            }
            case CONFIRM_COMPLETION -> {
                if (order.getRequesterUserId() != actor.id()) {
                    throw new OrderModuleException(HttpStatus.FORBIDDEN, "Only the requester can confirm completion.");
                }
                if (order.getStatus() != OrderStatus.PENDING_CONFIRMATION) {
                    throw new OrderModuleException(HttpStatus.BAD_REQUEST, "This order is not ready to be confirmed.");
                }
                order.updateStatus(OrderStatus.COMPLETED, now);
                order.addTimelineEntry(new OrderTimelineEntry(now, actor.name(), "Requester confirmed completion and closed the order."));
                request.markCompleted();
                repository.storeRequest(request);
            }
            case REJECT_COMPLETION -> {
                if (order.getRequesterUserId() != actor.id()) {
                    throw new OrderModuleException(HttpStatus.FORBIDDEN, "Only the requester can reject completion.");
                }
                if (order.getStatus() != OrderStatus.PENDING_CONFIRMATION) {
                    throw new OrderModuleException(HttpStatus.BAD_REQUEST, "This order is not waiting for completion confirmation.");
                }
                if (note.isBlank()) {
                    throw new OrderModuleException(HttpStatus.BAD_REQUEST, "A rejection note is required.");
                }
                order.updateStatus(OrderStatus.IN_PROGRESS, now);
                order.setLatestRequesterNote(note);
                order.addTimelineEntry(new OrderTimelineEntry(now, actor.name(), "Requester rejected completion: " + note));
            }
        }

        repository.storeOrder(order);
        return getOrderDetail(orderId, actorId);
    }

    private boolean shouldShowRequest(HelpRequest request, Long viewerId) {
        if (request.getStatus() == HelpRequestStatus.OPEN) {
            return true;
        }
        if (viewerId == null) {
            return false;
        }
        if (request.getRequesterUserId() == viewerId) {
            return request.getStatus() != HelpRequestStatus.COMPLETED;
        }
        return request.getApplications().stream()
                .anyMatch(application -> application.getApplicantUserId() == viewerId);
    }

    private boolean matchesCategory(HelpRequest request, String category) {
        if (category == null || category.isBlank() || "all".equalsIgnoreCase(category)) {
            return true;
        }
        return request.getCategory().equalsIgnoreCase(category.trim());
    }

    private RequestCardView toRequestCard(HelpRequest request, Long viewerId) {
        DemoUser requester = requireUser(request.getRequesterUserId());
        boolean ownedByViewer = viewerId != null && request.getRequesterUserId() == viewerId;
        boolean viewerApplied = viewerId != null && request.getApplications().stream()
                .anyMatch(application -> application.getApplicantUserId() == viewerId
                        && application.getStatus() != ApplicationStatus.REJECTED);

        return new RequestCardView(
                request.getId(),
                request.getTitle(),
                request.getCategory(),
                request.getLocation(),
                request.getReward(),
                request.getServiceTime(),
                request.getCreatedAt(),
                request.getStatus().name(),
                request.getStatus().getLabel(),
                requester.name(),
                request.getApplications().size(),
                ownedByViewer,
                viewerId != null && request.getStatus() == HelpRequestStatus.OPEN && !ownedByViewer && !viewerApplied,
                request.getCurrentOrderId()
        );
    }

    private RequestDetailView toRequestDetail(HelpRequest request, Long viewerId) {
        DemoUser requester = requireUser(request.getRequesterUserId());
        boolean manageApplications = viewerId != null && request.getRequesterUserId() == viewerId;

        List<ApplicationView> applications = request.getApplications().stream()
                .filter(application -> manageApplications || (viewerId != null && application.getApplicantUserId() == viewerId))
                .sorted(Comparator.comparing(RequestApplication::getCreatedAt))
                .map(application -> {
                    DemoUser applicant = requireUser(application.getApplicantUserId());
                    boolean selectable = manageApplications
                            && request.getStatus() == HelpRequestStatus.OPEN
                            && application.getStatus() == ApplicationStatus.PENDING;
                    return new ApplicationView(
                            application.getId(),
                            applicant.id(),
                            applicant.name(),
                            applicant.academy(),
                            application.getMessage(),
                            application.getStatus().name(),
                            application.getStatus().getLabel(),
                            application.getCreatedAt(),
                            selectable
                    );
                })
                .toList();

        String selectedProviderName = null;
        if (request.getCurrentOrderId() != null) {
            ServiceOrder currentOrder = requireOrder(request.getCurrentOrderId());
            selectedProviderName = requireUser(currentOrder.getProviderUserId()).name();
        }

        return new RequestDetailView(
                request.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getLocation(),
                request.getReward(),
                request.getServiceTime(),
                request.getCreatedAt(),
                request.getStatus().name(),
                request.getStatus().getLabel(),
                requester.id(),
                requester.name(),
                manageApplications,
                viewerId != null && request.getStatus() == HelpRequestStatus.OPEN && request.getRequesterUserId() != viewerId,
                request.getCurrentOrderId(),
                selectedProviderName,
                applications
        );
    }

    private OrderSummaryView toOrderSummary(ServiceOrder order, long viewerId) {
        HelpRequest request = requireRequest(order.getRequestId());
        DemoUser requester = requireUser(order.getRequesterUserId());
        DemoUser provider = requireUser(order.getProviderUserId());
        String counterpartName = order.getRequesterUserId() == viewerId ? provider.name() : requester.name();

        return new OrderSummaryView(
                order.getId(),
                request.getTitle(),
                request.getCategory(),
                order.getStatus().name(),
                order.getStatus().getLabel(),
                counterpartName,
                order.getUpdatedAt(),
                request.getCurrentOrderId() != null && request.getCurrentOrderId() == order.getId()
        );
    }

    private OrderDetailView toOrderDetail(ServiceOrder order, long viewerId) {
        HelpRequest request = requireRequest(order.getRequestId());
        DemoUser requester = requireUser(order.getRequesterUserId());
        DemoUser provider = requireUser(order.getProviderUserId());

        List<ActionView> availableActions = switch (order.getStatus()) {
            case PENDING_ACCEPTANCE -> order.getProviderUserId() == viewerId
                    ? List.of(new ActionView("START", "Start Task", false, ""))
                    : List.of();
            case IN_PROGRESS -> order.getProviderUserId() == viewerId
                    ? List.of(new ActionView("SUBMIT_COMPLETION", "Submit Completion", false, ""))
                    : List.of();
            case PENDING_CONFIRMATION -> order.getRequesterUserId() == viewerId
                    ? List.of(
                    new ActionView("CONFIRM_COMPLETION", "Confirm Completion", false, ""),
                    new ActionView("REJECT_COMPLETION", "Reject Completion", true, "Explain why the result is rejected")
            ) : List.of();
            case COMPLETED -> List.of();
        };

        List<TimelineView> timeline = order.getTimelineEntries().stream()
                .sorted(Comparator.comparing(OrderTimelineEntry::happenedAt))
                .map(entry -> new TimelineView(entry.happenedAt(), entry.actorName(), entry.description()))
                .toList();

        return new OrderDetailView(
                order.getId(),
                request.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getLocation(),
                request.getReward(),
                request.getServiceTime(),
                order.getStatus().name(),
                order.getStatus().getLabel(),
                requester.name(),
                provider.name(),
                order.getUpdatedAt(),
                order.getCompletedAt(),
                order.getLatestRequesterNote(),
                availableActions,
                timeline
        );
    }

    private DemoUser requireUser(long userId) {
        return repository.findUserById(userId)
                .orElseThrow(() -> new OrderModuleException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private HelpRequest requireRequest(long requestId) {
        return repository.findRequestById(requestId)
                .orElseThrow(() -> new OrderModuleException(HttpStatus.NOT_FOUND, "Request not found: " + requestId));
    }

    private ServiceOrder requireOrder(long orderId) {
        return repository.findOrderById(orderId)
                .orElseThrow(() -> new OrderModuleException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));
    }

    private void requireParticipant(ServiceOrder order, long userId) {
        if (order.getRequesterUserId() != userId && order.getProviderUserId() != userId) {
            throw new OrderModuleException(HttpStatus.FORBIDDEN, "Only order participants can view or operate on this order.");
        }
    }

    private String normalizeMessage(String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isBlank()) {
            throw new OrderModuleException(HttpStatus.BAD_REQUEST, "Application message cannot be empty.");
        }
        if (message.length() > 120) {
            throw new OrderModuleException(HttpStatus.BAD_REQUEST, "Application message cannot exceed 120 characters.");
        }
        return message;
    }

    public record UserView(long id, String name, String academy, String roleLabel) {
    }

    public record RequestCardView(
            long id,
            String title,
            String category,
            String location,
            String reward,
            LocalDateTime serviceTime,
            LocalDateTime createdAt,
            String status,
            String statusLabel,
            String requesterName,
            int applicationCount,
            boolean ownedByViewer,
            boolean canApply,
            Long currentOrderId
    ) {
    }

    public record ApplicationView(
            long id,
            long applicantId,
            String applicantName,
            String applicantAcademy,
            String message,
            String status,
            String statusLabel,
            LocalDateTime createdAt,
            boolean selectable
    ) {
    }

    public record RequestDetailView(
            long id,
            String title,
            String description,
            String category,
            String location,
            String reward,
            LocalDateTime serviceTime,
            LocalDateTime createdAt,
            String status,
            String statusLabel,
            long requesterId,
            String requesterName,
            boolean canManageApplications,
            boolean canApply,
            Long currentOrderId,
            String selectedProviderName,
            List<ApplicationView> applications
    ) {
    }

    public record OrderSummaryView(
            long id,
            String requestTitle,
            String category,
            String status,
            String statusLabel,
            String counterpartName,
            LocalDateTime updatedAt,
            boolean active
    ) {
    }

    public record ActionView(String code, String label, boolean requiresNote, String notePlaceholder) {
    }

    public record TimelineView(LocalDateTime happenedAt, String actorName, String description) {
    }

    public record OrderDetailView(
            long id,
            long requestId,
            String requestTitle,
            String requestDescription,
            String location,
            String reward,
            LocalDateTime serviceTime,
            String status,
            String statusLabel,
            String requesterName,
            String providerName,
            LocalDateTime updatedAt,
            LocalDateTime completedAt,
            String latestRequesterNote,
            List<ActionView> availableActions,
            List<TimelineView> timeline
    ) {
    }
}
