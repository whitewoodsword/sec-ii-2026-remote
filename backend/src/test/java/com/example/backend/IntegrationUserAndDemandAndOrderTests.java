package com.example.backend;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public class IntegrationUserAndDemandAndOrderTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private OrderRepository orderRepository;

    private String testUserToken;
    private Long testUserId;
    private Long testDemandId;
    private Long testOrderId;
    private Long testAcceptorId;

    // 记录测试过程中创建的所有用户ID、需求ID、订单ID
    private List<Long> createdUserIds = new ArrayList<>();
    private List<Long> createdDemandIds = new ArrayList<>();
    private List<Long> createdOrderIds = new ArrayList<>();

    // 记录测试手机号，用于清理
    private List<String> createdPhones = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 手动构建 MockMvc
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        // 手动创建 ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @AfterEach
    void cleanUp() {
        // 清理订单
        for (Long orderId : createdOrderIds) {
            try {
                if (orderId != null && orderRepository.existsById(orderId)) {
                    orderRepository.deleteById(orderId);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete order " + orderId + ": " + e.getMessage());
            }
        }

        // 清理需求
        for (Long demandId : createdDemandIds) {
            try {
                if (demandId != null && demandRepository.existsById(demandId)) {
                    demandRepository.deleteById(demandId);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete demand " + demandId + ": " + e.getMessage());
            }
        }

        // 清理用户（注意顺序：先删除关联的订单和需求，然后删除用户）
        for (Long userId : createdUserIds) {
            try {
                if (userId != null && userRepository.existsById(userId)) {
                    // 先删除该用户发布的需求（如果还没被删除）
                    List<Demand> userDemands = demandRepository.findByPublisherId(userId, Pageable.unpaged()).getContent();
                    for (Demand demand : userDemands) {
                        if (!createdDemandIds.contains(demand.getId())) {
                            demandRepository.deleteById(demand.getId());
                        }
                    }
                    // 删除用户
                    userRepository.deleteById(userId);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete user " + userId + ": " + e.getMessage());
            }
        }

        // 清理通过手机号注册的额外用户
        for (String phone : createdPhones) {
            try {
                userRepository.findByPhone(phone).ifPresent(user -> {
                    if (!createdUserIds.contains(user.getId())) {
                        userRepository.delete(user);
                    }
                });
            } catch (Exception e) {
                System.err.println("Failed to delete user with phone " + phone + ": " + e.getMessage());
            }
        }

        // 清空记录列表
        createdUserIds.clear();
        createdDemandIds.clear();
        createdOrderIds.clear();
        createdPhones.clear();

        // 重置测试变量
        testUserToken = null;
        testUserId = null;
        testDemandId = null;
        testOrderId = null;
        testAcceptorId = null;
    }

    // ==================== 用户模块集成测试 ====================

    @Test
    void test01UserRegistrationAndLogin() throws Exception {
        // 1. 注册新用户
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("phone", "13800138000");
        registerRequest.put("password", "password123");

        createdPhones.add("13800138000");

        MvcResult registerResult = mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").exists())
                .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();
        Long userId = objectMapper.readTree(registerResponse).path("data").path("userId").asLong();
        assertThat(userId).isNotNull();
        createdUserIds.add(userId);

        // 2. 登录获取token
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("phone", "13800138000");
        loginRequest.put("password", "password123");

        MvcResult loginResult = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.id").value(userId))
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        testUserToken = objectMapper.readTree(loginResponse).path("data").path("token").asText();
        testUserId = userId;

        // 3. 获取用户信息
        mockMvc.perform(get("/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));
    }

    @Test
    void test02UserRegistrationWithDuplicatePhone() throws Exception {
        // 先创建一个用户
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("phone", "13800138001");
        registerRequest.put("password", "password123");
        createdPhones.add("13800138001");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 使用已注册的手机号再次注册
        Map<String, String> duplicateUser = new HashMap<>();
        duplicateUser.put("phone", "13800138001");
        duplicateUser.put("password", "pass456");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("手机号已注册")));
    }

    @Test
    void test03UserLoginWithWrongPassword() throws Exception {
        // 先创建一个用户
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("phone", "13800138002");
        registerRequest.put("password", "password123");
        createdPhones.add("13800138002");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 使用已注册的手机号，错误密码登录
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("phone", "13800138002");
        loginRequest.put("password", "wrongPass");

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value(containsString("密码错误")));
    }

    @Test
    void test04GetUserRanking() throws Exception {
        // 创建更多带评分的用户
        createUserWithScore("13811111111", "user1", 4.5, 10L);
        createUserWithScore("13822222222", "user2", 4.8, 8L);
        createUserWithScore("13833333333", "user3", 4.2, 12L);

        createdPhones.add("13811111111");
        createdPhones.add("13822222222");
        createdPhones.add("13833333333");

        // 获取排行榜
        mockMvc.perform(get("/users/ranking")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    void test05GetPlatformStatistics() throws Exception {
        mockMvc.perform(get("/users/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").exists());
    }

    @Test
    void test06GetUserById() throws Exception {
        // 需要先有 testUserId，所以先调用 test01
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }
        mockMvc.perform(get("/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testUserId));
    }

    @Test
    void test07ListUsers() throws Exception {
        mockMvc.perform(get("/users/list")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ==================== 需求模块集成测试 ====================

    @Test
    void test08CreateAndGetDemand() throws Exception {
        // 确保有测试用户
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }

        // 创建需求
        Demand demand = createTestDemandObject();

        MvcResult createResult = mockMvc.perform(post("/demands/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("测试需求"))
                .andExpect(jsonPath("$.data.category").value("生活服务"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        testDemandId = objectMapper.readTree(createResponse).path("data").path("id").asLong();
        createdDemandIds.add(testDemandId);

        // 获取需求详情
        mockMvc.perform(get("/demands/{id}", testDemandId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("测试需求"))
                .andExpect(jsonPath("$.data.publisherId").value(testUserId));
    }

    @Test
    void test09SearchDemands() throws Exception {
        // 确保有测试用户
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }

        // 创建更多需求
        Demand demand1 = createTestDemandObject();
        demand1.setTitle("Java开发需求");
        demand1.setReward(500.0);
        demand1.setPublisherId(testUserId);
        demand1.setCategory("其他");
        demand1.setStatus("PENDING");
        demand1.setCreatedAt(LocalDateTime.now());
        demand1.setUpdatedAt(LocalDateTime.now());
        Demand savedDemand1 = demandRepository.save(demand1);
        createdDemandIds.add(savedDemand1.getId());

        Demand demand2 = createTestDemandObject();
        demand2.setTitle("UI设计需求");
        demand2.setCategory("学习辅导");
        demand2.setReward(38888.0);
        demand2.setStatus("PENDING");
        demand2.setPublisherId(testUserId);
        demand2.setCreatedAt(LocalDateTime.now());
        demand2.setUpdatedAt(LocalDateTime.now());
        Demand savedDemand2 = demandRepository.save(demand2);
        createdDemandIds.add(savedDemand2.getId());

        // 按关键词搜索
        mockMvc.perform(get("/demands/search")
                        .param("keyword", "Java开发需求")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Java开发需求"));

        // 添加按分类的搜索
        mockMvc.perform(get("/demands/search")
                        .param("keyword", "UI设计需求")
                        .param("category", "学习辅导"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].category").value("学习辅导"));

        // 按价格范围搜索
        mockMvc.perform(get("/demands/search")
                        .param("minReward", "38888")
                        .param("maxReward", "38888")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void test10UpdateDemandStatus() throws Exception {
        // 确保有需求和测试用户
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }
        if (testDemandId == null) {
            test08CreateAndGetDemand();
        }

        // 将需求状态改为ACCEPTED
        mockMvc.perform(patch("/demands/{id}/status", testDemandId)
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证状态已更新
        mockMvc.perform(get("/demands/{id}", testDemandId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void test11UpdateDemandWithInvalidStatus() throws Exception {
        // 确保有需求和测试用户
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }
        if (testDemandId == null) {
            test08CreateAndGetDemand();
        }

        mockMvc.perform(patch("/demands/{id}/status", testDemandId)
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("无效的状态值")));
    }

    @Test
    void test12GetActiveDemands() throws Exception {
        // 确保有测试用户
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }

        // 创建另一个PENDING状态的需求
        Demand anotherDemand = createTestDemandObject();
        anotherDemand.setTitle("另一个活跃需求");
        anotherDemand.setStatus("PENDING");
        anotherDemand.setCreatedAt(LocalDateTime.now());
        anotherDemand.setUpdatedAt(LocalDateTime.now());
        anotherDemand.setPublisherId(testUserId);
        anotherDemand.setCategory("其他");
        Demand savedDemand = demandRepository.save(anotherDemand);
        createdDemandIds.add(savedDemand.getId());

        mockMvc.perform(get("/demands/active")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    void test13GetCategories() throws Exception {
        mockMvc.perform(get("/demands/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(8)));
    }

    @Test
    void test14GetDemandsByPublisher() throws Exception {
        // 确保有测试用户
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }

        mockMvc.perform(get("/demands/publisher/{publisherId}", testUserId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    // ==================== 订单模块集成测试 ====================

    @Test
    void test15CreateOrder() throws Exception {
        // 确保有测试用户
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }

        // 创建接单者
        User acceptor = createUser("13612345678", "acceptor");
        testAcceptorId = acceptor.getId();
        createdUserIds.add(testAcceptorId);

        // 创建一个新需求用于订单测试
        Demand newDemand = createTestDemandObject();
        newDemand.setTitle("订单测试需求");
        newDemand.setStatus("PENDING");
        newDemand.setDescription("无");
        newDemand.setCategory("其他");
        newDemand.setPublisherId(testUserId);
        newDemand.setCreatedAt(LocalDateTime.now());
        newDemand.setUpdatedAt(LocalDateTime.now());
        Demand savedDemand = demandRepository.save(newDemand);
        Long newDemandId = savedDemand.getId();
        createdDemandIds.add(newDemandId);

        // 创建订单
        mockMvc.perform(post("/orders/create")
                        .param("demandId", String.valueOf(newDemandId))
                        .param("userId", String.valueOf(testAcceptorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.demandId").value(newDemandId))
                .andExpect(jsonPath("$.data.publisherId").value(testUserId))
                .andExpect(jsonPath("$.data.acceptorId").value(testAcceptorId))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // 保存订单ID供后续测试使用
        MvcResult orderResult = mockMvc.perform(get("/orders/demand/{demandId}", newDemandId))
                .andReturn();
        if (orderResult.getResponse().getStatus() == 200) {
            String orderResponse = orderResult.getResponse().getContentAsString();
            testOrderId = objectMapper.readTree(orderResponse).path("data").path("id").asLong();
            createdOrderIds.add(testOrderId);
        }

        // 保存需求ID
        testDemandId = newDemandId;
    }

    @Test
    void test16CreateOrderForAlreadyAcceptedDemand() throws Exception {
        // 确保有订单
        if (testOrderId == null) {
            test15CreateOrder();
        }

        // 尝试用同一个需求再次创建订单（应该失败）
        Long anotherAcceptor = createUser("13622222222", "acceptor2").getId();
        createdUserIds.add(anotherAcceptor);

        mockMvc.perform(post("/orders/create")
                        .param("demandId", String.valueOf(testDemandId))
                        .param("userId", String.valueOf(anotherAcceptor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("该需求当前状态为ACCEPTED，无法接单")));
    }

    @Test
    void test17GetOrderById() throws Exception {
        // 确保有订单
        if (testOrderId == null) {
            test15CreateOrder();
        }

        mockMvc.perform(get("/orders/{id}", testOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testOrderId));
    }

    @Test
    void test18GetOrderByDemandId() throws Exception {
        // 确保有订单
        if (testOrderId == null) {
            test15CreateOrder();
        }

        mockMvc.perform(get("/orders/demand/{demandId}", testDemandId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.demandId").value(testDemandId));
    }

    @Test
    void test19UpdateOrderStatus() throws Exception {
        // 确保有订单
        if (testOrderId == null) {
            test15CreateOrder();
        }

        // 更新订单状态为IN_PROGRESS
        mockMvc.perform(patch("/orders/{id}/status", testOrderId)
                        .param("userId", String.valueOf(testUserId))
                        .param("status", "IN_PROGRESS")
                        .param("note", "开始工作"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void test20UpdateOrderNote() throws Exception {
        // 确保有订单
        if (testOrderId == null) {
            test15CreateOrder();
        }

        mockMvc.perform(patch("/orders/{id}/note", testOrderId)
                        .param("userId", String.valueOf(testUserId))
                        .param("note", "更新备注信息"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void test21CannotCompleteOrderDirectly() throws Exception {
        // 确保有订单和接单者
        if (testOrderId == null) {
            test15CreateOrder();
        }
        if (testAcceptorId == null) {
            test15CreateOrder();
        }

        // 无法直接完成订单
        mockMvc.perform(post("/orders/{id}/complete", testOrderId)
                        .param("userId", String.valueOf(testAcceptorId))
                        .param("commentId", "1"))
                .andExpect(jsonPath("$.code").value(400));

        // 验证订单状态
        Order completedOrder = orderRepository.findById(testOrderId).get();
        assertThat(completedOrder.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void test22GetOrdersAsPublisher() throws Exception {
        // 确保有测试用户和订单
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }

        mockMvc.perform(get("/orders/publisher/{publisherId}", testUserId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    void test23GetOrdersAsAcceptor() throws Exception {
        // 确保有接单者
        if (testAcceptorId == null) {
            test15CreateOrder();
        }

        mockMvc.perform(get("/orders/acceptor/{acceptorId}", testAcceptorId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    void test24GetAllUserOrders() throws Exception {
        // 确保有测试用户
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }

        mockMvc.perform(get("/orders/user/{userId}", testUserId)
                        .param("page", "0")
                        .param("size", "10")
                        .param("role", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    void test25SearchOrders() throws Exception {
        mockMvc.perform(get("/orders/search")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").exists());

        // 按发布者ID搜索
        if (testUserId != null) {
            mockMvc.perform(get("/orders/search")
                            .param("publisherId", String.valueOf(testUserId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").exists());
        }
    }

    @Test
    void test26GetOrderStatistics() throws Exception {
        // 确保有测试用户
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }

        mockMvc.perform(get("/orders/statistics/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalOrders").exists());
    }

    @Test
    void test27GetValidOrderStatuses() throws Exception {
        mockMvc.perform(get("/orders/statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data", containsInAnyOrder(
                        "ACCEPTED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "DISPUTED")));
    }

    @Test
    void test28CancelNewOrder() throws Exception {
        // 确保有测试用户
        if (testUserId == null) {
            test01UserRegistrationAndLogin();
        }

        // 创建一个新需求和新订单用于测试取消
        Demand newDemand = createTestDemandObject();
        newDemand.setTitle("可取消的需求");
        newDemand.setStatus("PENDING");
        newDemand.setCategory("其他");
        newDemand.setCreatedAt(LocalDateTime.now());
        newDemand.setUpdatedAt(LocalDateTime.now());
        newDemand.setPublisherId(testUserId);
        Demand savedDemand = demandRepository.save(newDemand);
        createdDemandIds.add(savedDemand.getId());

        User newAcceptor = createUser("13633333333", "acceptor3");
        createdUserIds.add(newAcceptor.getId());

        Order newOrder = new Order(savedDemand.getId(), testUserId, newAcceptor.getId());
        Order savedOrder = orderRepository.save(newOrder);
        createdOrderIds.add(savedOrder.getId());

        // 取消订单
        mockMvc.perform(post("/orders/{id}/cancel", savedOrder.getId())
                        .param("userId", String.valueOf(testUserId))
                        .param("reason", "测试取消"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // ==================== 跨模块业务流程集成测试 ====================

    @Test
    void test29CompleteBusinessFlow() throws Exception {
        // 1. 注册新发布者
        Map<String, String> publisherReg = new HashMap<>();
        publisherReg.put("phone", "15011111111");
        publisherReg.put("password", "pub123");
        createdPhones.add("15011111111");

        MvcResult pubResult = mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publisherReg)))
                .andReturn();
        Long newPublisherId = objectMapper.readTree(pubResult.getResponse().getContentAsString())
                .path("data").path("userId").asLong();
        createdUserIds.add(newPublisherId);

        // 2. 注册新接单者
        Map<String, String> acceptorReg = new HashMap<>();
        acceptorReg.put("phone", "15022222222");
        acceptorReg.put("password", "acc123");
        createdPhones.add("15022222222");

        MvcResult accResult = mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptorReg)))
                .andReturn();
        Long newAcceptorId = objectMapper.readTree(accResult.getResponse().getContentAsString())
                .path("data").path("userId").asLong();
        createdUserIds.add(newAcceptorId);

        // 3. 发布新需求
        Demand demand = new Demand();
        demand.setTitle("完整流程测试需求");
        demand.setDescription("测试从发布到完成的完整流程");
        demand.setCategory("生活服务");
        demand.setPublisherId(newPublisherId);
        demand.setLocation("测试地点");
        demand.setDeadline(LocalDateTime.now().plusDays(7));
        demand.setReward(500.0);

        MvcResult demandResult = mockMvc.perform(post("/demands/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demand)))
                .andExpect(status().isCreated())
                .andReturn();
        Long newDemandId = objectMapper.readTree(demandResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        createdDemandIds.add(newDemandId);

        // 4. 接单者创建订单
        MvcResult orderResult = mockMvc.perform(post("/orders/create")
                        .param("demandId", String.valueOf(newDemandId))
                        .param("userId", String.valueOf(newAcceptorId)))
                .andExpect(status().isCreated())
                .andReturn();
        Long newOrderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        createdOrderIds.add(newOrderId);

        // 5. 发布者确认开始（更新状态为IN_PROGRESS）
        mockMvc.perform(patch("/orders/{id}/status", newOrderId)
                        .param("userId", String.valueOf(newPublisherId))
                        .param("status", "IN_PROGRESS")
                        .param("note", "请开始工作"))
                .andExpect(status().isOk());

        // 6. 接单者完成订单
        mockMvc.perform(post("/orders/{id}/complete", newOrderId)
                        .param("userId", String.valueOf(newPublisherId))
                        .param("commentId", "1"))
                .andExpect(status().isOk());

        // 7. 验证最终状态
        mockMvc.perform(get("/orders/{id}", newOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    // ==================== 辅助方法 ====================

    private User createUser(String phone, String name) {
        if (userRepository.existsByPhone(phone)) {
            return userRepository.findByPhone(phone).get();
        }
        User user = new User(name, phone, "password123");
        User savedUser = userRepository.save(user);
        createdUserIds.add(savedUser.getId());
        return savedUser;
    }

    private User createUserWithScore(String phone, String name, Double avgScore, Long scoreNum) {
        User user = createUser(phone, name);
        user.setAverageScore(avgScore);
        user.setScoreNum(scoreNum);
        User savedUser = userRepository.save(user);
        return savedUser;
    }

    private Demand createTestDemandObject() {
        Demand demand = new Demand();
        demand.setTitle("测试需求");
        demand.setDescription("这是一个测试需求描述");
        demand.setCategory("生活服务");
        demand.setPublisherId(testUserId);
        demand.setLocation("测试地点");
        demand.setDeadline(LocalDateTime.now().plusDays(7));
        demand.setReward(100.0);
        demand.setPictureUrls("test.jpg");
        return demand;
    }
}
