package com.example.backend;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.backend.entity.Demand;
import com.example.backend.entity.Order;
import com.example.backend.entity.User;
import com.example.backend.repository.DemandRepository;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
class OrderModuleIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        orderRepository.deleteAll();
        demandRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createOrder_updatesOrderAndDemand() throws Exception {
        User publisher = userRepository.save(new User("publisher", "18800000001", "password"));
        User acceptor = userRepository.save(new User("acceptor", "18800000002", "password"));

        Demand demand = new Demand();
        demand.setTitle("Test demand");
        demand.setDescription("Need help");
        demand.setCategory("生活服务");
        demand.setPublisherId(publisher.getId());
        demand.setStatus("PENDING");
        demand.setLocation("NJU");
        demand.setDeadline(LocalDateTime.now().plusDays(1));
        demand.setReward(10.0);
        demand.setCreatedAt(LocalDateTime.now());
        demand.setUpdatedAt(LocalDateTime.now());
        Demand savedDemand = demandRepository.save(demand);

        mockMvc.perform(post("/orders/create")
                        .param("demandId", savedDemand.getId().toString())
                        .param("userId", acceptor.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.demandId").value(savedDemand.getId()))
                .andExpect(jsonPath("$.data.publisherId").value(publisher.getId()))
                .andExpect(jsonPath("$.data.acceptorId").value(acceptor.getId()))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        Demand updatedDemand = demandRepository.findById(savedDemand.getId()).orElseThrow();
        Order createdOrder = orderRepository.findByDemandId(savedDemand.getId()).orElseThrow();

        assertThat(updatedDemand.getStatus()).isEqualTo("ACCEPTED");
        assertThat(updatedDemand.getOrderId()).isEqualTo(createdOrder.getId());
        assertThat(createdOrder.getPublisherId()).isEqualTo(publisher.getId());
        assertThat(createdOrder.getAcceptorId()).isEqualTo(acceptor.getId());
    }

    @Test
    void updateOrderStatus_movesAcceptedOrderToInProgress() throws Exception {
        User publisher = userRepository.save(new User("publisher", "18800000003", "password"));
        User acceptor = userRepository.save(new User("acceptor", "18800000004", "password"));

        Demand demand = new Demand();
        demand.setTitle("Accepted demand");
        demand.setDescription("Need help");
        demand.setCategory("生活服务");
        demand.setPublisherId(publisher.getId());
        demand.setStatus("ACCEPTED");
        demand.setLocation("NJU");
        demand.setDeadline(LocalDateTime.now().plusDays(1));
        demand.setReward(12.0);
        demand.setCreatedAt(LocalDateTime.now());
        demand.setUpdatedAt(LocalDateTime.now());
        Demand savedDemand = demandRepository.save(demand);

        Order order = new Order(savedDemand.getId(), publisher.getId(), acceptor.getId());
        order.setStatus("ACCEPTED");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        savedDemand.setOrderId(savedOrder.getId());
        demandRepository.save(savedDemand);

        mockMvc.perform(patch("/orders/{id}/status", savedOrder.getId())
                        .param("userId", acceptor.getId().toString())
                        .param("status", "IN_PROGRESS")
                        .param("note", "starting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.latestRequesterNote").value("starting"));

        Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        Demand updatedDemand = demandRepository.findById(savedDemand.getId()).orElseThrow();

        assertThat(updatedOrder.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(updatedDemand.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void cancelOrder_resetsDemandStateAndUnbindsOrder() throws Exception {
        User publisher = userRepository.save(new User("publisher", "18800000005", "password"));
        User acceptor = userRepository.save(new User("acceptor", "18800000006", "password"));

        Demand demand = new Demand();
        demand.setTitle("Cancelable demand");
        demand.setDescription("Need help");
        demand.setCategory("生活服务");
        demand.setPublisherId(publisher.getId());
        demand.setStatus("ACCEPTED");
        demand.setLocation("NJU");
        demand.setDeadline(LocalDateTime.now().plusDays(1));
        demand.setReward(15.0);
        demand.setCreatedAt(LocalDateTime.now());
        demand.setUpdatedAt(LocalDateTime.now());
        Demand savedDemand = demandRepository.save(demand);

        Order order = new Order(savedDemand.getId(), publisher.getId(), acceptor.getId());
        order.setStatus("ACCEPTED");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        savedDemand.setOrderId(savedOrder.getId());
        demandRepository.save(savedDemand);

        mockMvc.perform(post("/orders/{id}/cancel", savedOrder.getId())
                        .param("userId", publisher.getId().toString())
                        .param("reason", "publisher cancelled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.latestRequesterNote").value("publisher cancelled"));

        Order cancelledOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        Demand updatedDemand = demandRepository.findById(savedDemand.getId()).orElseThrow();

        assertThat(cancelledOrder.getStatus()).isEqualTo("CANCELLED");
        assertThat(updatedDemand.getStatus()).isEqualTo("PENDING");
        assertThat(updatedDemand.getOrderId()).isNull();
    }

    @Test
    void completeOrder_marksOrderAndDemandCompleted() throws Exception {
        User publisher = userRepository.save(new User("publisher", "18800000007", "password"));
        User acceptor = userRepository.save(new User("acceptor", "18800000008", "password"));

        Demand demand = new Demand();
        demand.setTitle("Completable demand");
        demand.setDescription("Need help");
        demand.setCategory("生活服务");
        demand.setPublisherId(publisher.getId());
        demand.setStatus("ACCEPTED");
        demand.setLocation("NJU");
        demand.setDeadline(LocalDateTime.now().plusDays(1));
        demand.setReward(18.0);
        demand.setCreatedAt(LocalDateTime.now());
        demand.setUpdatedAt(LocalDateTime.now());
        Demand savedDemand = demandRepository.save(demand);

        Order order = new Order(savedDemand.getId(), publisher.getId(), acceptor.getId());
        order.setStatus("IN_PROGRESS");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        savedDemand.setOrderId(savedOrder.getId());
        demandRepository.save(savedDemand);

        mockMvc.perform(post("/orders/{id}/complete", savedOrder.getId())
                        .param("userId", publisher.getId().toString())
                        .param("commentId", "9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.commentId").value(9001));

        Order completedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        Demand updatedDemand = demandRepository.findById(savedDemand.getId()).orElseThrow();

        assertThat(completedOrder.getStatus()).isEqualTo("COMPLETED");
        assertThat(completedOrder.getCompletedAt()).isNotNull();
        assertThat(updatedDemand.getStatus()).isEqualTo("COMPLETED");
    }
}
