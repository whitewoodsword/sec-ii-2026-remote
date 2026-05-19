package com.example.backend.config;

import com.example.backend.entity.Demand;
import com.example.backend.entity.DemandApplication;
import com.example.backend.entity.Order;
import com.example.backend.entity.OrderTimelineRecord;
import com.example.backend.entity.User;
import com.example.backend.repository.DemandApplicationRepository;
import com.example.backend.repository.DemandRepository;
import com.example.backend.repository.OrderEntityRepository;
import com.example.backend.repository.OrderTimelineRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Component
@org.springframework.core.annotation.Order(2)
public class OrderDemoDataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final DemandRepository demandRepository;
    private final DemandApplicationRepository demandApplicationRepository;
    private final OrderEntityRepository orderEntityRepository;
    private final OrderTimelineRepository orderTimelineRepository;

    public OrderDemoDataInitializer(
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
    @Transactional
    public void run(String... args) {
        if (demandRepository.count() > 0 || orderEntityRepository.count() > 0 || demandApplicationRepository.count() > 0) {
            return;
        }

        User yingchi = ensureUser("Yingchi Liu", "18800000001");
        User yuchuan = ensureUser("Yuchuan Liu", "18800000002");
        User junda = ensureUser("Junda Lu", "18800000003");
        User boyi = ensureUser("Boyi Sun", "18800000004");

        Demand request1 = saveDemand(
                "Pick up a courier at Xianlin station",
                "Please collect one parcel before 20:00 and leave it at Dorm 7 downstairs.",
                "快递代取",
                yingchi.getId(),
                "Xianlin Cainiao Station",
                LocalDateTime.of(2026, 5, 18, 20, 0),
                12.0,
                LocalDateTime.of(2026, 5, 18, 9, 10)
        );
        saveApplication(request1.getId(), yuchuan.getId(), "I will be near the station tonight and can bring it back.", "PENDING", LocalDateTime.of(2026, 5, 18, 9, 30));
        saveApplication(request1.getId(), junda.getId(), "I finish class at 18:30 and can pick it up on the way.", "PENDING", LocalDateTime.of(2026, 5, 18, 10, 5));

        Demand request2 = saveDemand(
                "Print library materials and deliver to classroom",
                "Print 16 pages and deliver them to Building 2 Room 305 before 15:00.",
                "学习互助",
                boyi.getId(),
                "Library to Building 2 Room 305",
                LocalDateTime.of(2026, 5, 18, 15, 0),
                18.0,
                LocalDateTime.of(2026, 5, 18, 8, 20)
        );
        saveApplication(request2.getId(), yuchuan.getId(), "I am in the library this morning and can handle the print run.", "PENDING", LocalDateTime.of(2026, 5, 18, 8, 40));

        Demand request3 = saveDemand(
                "Milk tea group order handoff",
                "I will place the order around 19:00. Please bring my cup to the dorm gate.",
                "校园跑腿",
                boyi.getId(),
                "South Garden canteen",
                LocalDateTime.of(2026, 5, 18, 19, 0),
                8.0,
                LocalDateTime.of(2026, 5, 17, 21, 10)
        );
        saveApplication(
                request3.getId(),
                yuchuan.getId(),
                "I pass by South Garden after dinner and can drop it off.",
                "SELECTED",
                LocalDateTime.of(2026, 5, 18, 8, 55)
        );
        Order order1 = saveOrder(
                request3.getId(),
                boyi.getId(),
                yuchuan.getId(),
                "IN_PROGRESS",
                null,
                null,
                LocalDateTime.of(2026, 5, 18, 9, 0),
                LocalDateTime.of(2026, 5, 18, 9, 50)
        );
        linkDemandToOrder(request3, order1, "ACCEPTED", LocalDateTime.of(2026, 5, 18, 9, 50));
        saveTimeline(order1.getId(), "System", "Requester selected a provider and the order was created.", LocalDateTime.of(2026, 5, 18, 9, 0));
        saveTimeline(order1.getId(), yuchuan.getName(), "Provider started the task.", LocalDateTime.of(2026, 5, 18, 9, 50));

        Demand request4 = saveDemand(
                "Print and bind a lab report",
                "Please print 24 pages double-sided, bind them, and deliver to the lab building.",
                "学习互助",
                yingchi.getId(),
                "Copy shop",
                LocalDateTime.of(2026, 5, 19, 11, 30),
                20.0,
                LocalDateTime.of(2026, 5, 18, 7, 45)
        );
        saveApplication(
                request4.getId(),
                junda.getId(),
                "I will be near the copy shop in the morning and can deliver it quickly.",
                "SELECTED",
                LocalDateTime.of(2026, 5, 18, 8, 0)
        );
        Order order2 = saveOrder(
                request4.getId(),
                yingchi.getId(),
                junda.getId(),
                "PENDING_CONFIRMATION",
                null,
                null,
                LocalDateTime.of(2026, 5, 18, 8, 10),
                LocalDateTime.of(2026, 5, 18, 10, 35)
        );
        linkDemandToOrder(request4, order2, "ACCEPTED", LocalDateTime.of(2026, 5, 18, 10, 35));
        saveTimeline(order2.getId(), "System", "Requester selected a provider and the order was created.", LocalDateTime.of(2026, 5, 18, 8, 10));
        saveTimeline(order2.getId(), junda.getName(), "Provider started the task.", LocalDateTime.of(2026, 5, 18, 8, 30));
        saveTimeline(order2.getId(), junda.getName(), "Provider submitted completion and is waiting for confirmation.", LocalDateTime.of(2026, 5, 18, 10, 35));

        Demand request5 = saveDemand(
                "Collect a campus card and hand it over at the metro entrance",
                "Please collect the campus card before noon and hand it over at 12:30.",
                "校园跑腿",
                yuchuan.getId(),
                "Advisor office",
                LocalDateTime.of(2026, 5, 17, 12, 30),
                15.0,
                LocalDateTime.of(2026, 5, 17, 8, 20)
        );
        saveApplication(
                request5.getId(),
                boyi.getId(),
                "I am heading to the teaching building this morning and can bring it along.",
                "SELECTED",
                LocalDateTime.of(2026, 5, 17, 8, 35)
        );
        Order order3 = saveOrder(
                request5.getId(),
                yuchuan.getId(),
                boyi.getId(),
                "COMPLETED",
                LocalDateTime.of(2026, 5, 17, 12, 10),
                null,
                LocalDateTime.of(2026, 5, 17, 9, 0),
                LocalDateTime.of(2026, 5, 17, 12, 10)
        );
        linkDemandToOrder(request5, order3, "COMPLETED", LocalDateTime.of(2026, 5, 17, 12, 10));
        saveTimeline(order3.getId(), "System", "Requester selected a provider and the order was created.", LocalDateTime.of(2026, 5, 17, 9, 0));
        saveTimeline(order3.getId(), boyi.getName(), "Provider started the task.", LocalDateTime.of(2026, 5, 17, 9, 25));
        saveTimeline(order3.getId(), boyi.getName(), "Provider submitted completion and is waiting for confirmation.", LocalDateTime.of(2026, 5, 17, 11, 45));
        saveTimeline(order3.getId(), yuchuan.getName(), "Requester confirmed completion and closed the order.", LocalDateTime.of(2026, 5, 17, 12, 10));
    }

    private User ensureUser(String name, String phone) {
        return userRepository.findByPhone(phone).orElseGet(() -> {
            User user = new User();
            user.setName(name);
            user.setPhone(phone);
            user.setPassword(md5("123456"));
            user.setScoreNum(0L);
            user.setAverageScore(5.0);
            user.setAvatarPath(null);
            user.setToken(null);
            user.setAdmin(false);
            user.setSuperAdmin(false);
            return userRepository.save(user);
        });
    }

    private Demand saveDemand(
            String title,
            String description,
            String category,
            Long publisherId,
            String location,
            LocalDateTime deadline,
            Double reward,
            LocalDateTime createdAt
    ) {
        Demand demand = new Demand();
        demand.setTitle(title);
        demand.setDescription(description);
        demand.setCategory(category);
        demand.setPublisherId(publisherId);
        demand.setStatus("PENDING");
        demand.setLocation(location);
        demand.setDeadline(deadline);
        demand.setReward(reward);
        demand.setCreatedAt(createdAt);
        demand.setUpdatedAt(createdAt);
        demand.setOrderId(null);
        demand.setPictureUrls(null);
        return demandRepository.save(demand);
    }

    private void saveApplication(
            Long demandId,
            Long applicantUserId,
            String message,
            String status,
            LocalDateTime createdAt
    ) {
        DemandApplication application = new DemandApplication();
        application.setDemandId(demandId);
        application.setApplicantUserId(applicantUserId);
        application.setMessage(message);
        application.setStatus(status);
        application.setCreatedAt(createdAt);
        application.setUpdatedAt(createdAt);
        demandApplicationRepository.save(application);
    }

    private Order saveOrder(
            Long demandId,
            Long publisherId,
            Long acceptorId,
            String status,
            LocalDateTime completedAt,
            String latestRequesterNote,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        Order order = new Order();
        order.setDemandId(demandId);
        order.setPublisherId(publisherId);
        order.setAcceptorId(acceptorId);
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(updatedAt);
        order.setCompletedAt(completedAt);
        order.setLatestRequesterNote(latestRequesterNote);
        order.setCommentId(null);
        return orderEntityRepository.save(order);
    }

    private void linkDemandToOrder(Demand demand, Order order, String status, LocalDateTime updatedAt) {
        demand.setStatus(status);
        demand.setOrderId(order.getId());
        demand.setUpdatedAt(updatedAt);
        demandRepository.save(demand);
    }

    private void saveTimeline(Long orderId, String actorName, String description, LocalDateTime happenedAt) {
        OrderTimelineRecord record = new OrderTimelineRecord();
        record.setOrderId(orderId);
        record.setActorName(actorName);
        record.setDescription(description);
        record.setHappenedAt(happenedAt);
        orderTimelineRepository.save(record);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
