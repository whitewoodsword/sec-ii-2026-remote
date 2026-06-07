package com.example.backend.config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.backend.entity.Conversation;
import com.example.backend.entity.Demand;
import com.example.backend.entity.Message;
import com.example.backend.entity.Review;
import com.example.backend.entity.User;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.DemandRepository;
import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.repository.UserRepository;

@Component
@Profile("!test")
@Order(1)
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DemandRepository demandRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;

    // 固定分类
    private static final List<String> CATEGORIES = Arrays.asList("快递代取", "学习辅导", "二手交易", "活动组队", "其他");
    
    // 固定地点
    private static final List<String> LOCATIONS = Arrays.asList(
        "南区宿舍", "北区宿舍", "图书馆", "第一食堂", "第二食堂", 
        "教学楼A座", "教学楼B座", "体育馆", "游泳馆", "学生活动中心"
    );

    // 固定需求模板
    private static final List<DemandTemplate> DEMAND_TEMPLATES = Arrays.asList(
        new DemandTemplate("帮忙取快递", "快递在菜鸟驿站，帮忙送到宿舍", "快递代取", 8.0),
        new DemandTemplate("急！取大件快递", "行李箱太重，求帮忙搬上楼", "快递代取", 15.0),
        new DemandTemplate("代取多个快递", "有三个快递在快递站，求帮忙", "快递代取", 12.0),
        new DemandTemplate("高数辅导", "需要辅导微积分，期末复习", "学习辅导", 50.0),
        new DemandTemplate("Java编程辅导", "课程设计不会做，求指导", "学习辅导", 60.0),
        new DemandTemplate("英语四六级辅导", "英语基础差，求带", "学习辅导", 40.0),
        new DemandTemplate("出售二手电动车", "九成新，续航30km", "二手交易", 800.0),
        new DemandTemplate("二手教材出售", "计算机专业教材全套", "二手交易", 150.0),
        new DemandTemplate("吉他出售", "初学者吉他，八成新", "二手交易", 200.0),
        new DemandTemplate("羽毛球组队", "周末一起打球", "活动组队", 0.0),
        new DemandTemplate("图书馆占座", "帮忙占靠窗位置", "其他", 10.0),
        new DemandTemplate("上门喂猫", "周末出门，求帮忙喂猫", "其他", 30.0),
        new DemandTemplate("帮忙打印资料", "需要打印50页双面", "其他", 15.0),
        new DemandTemplate("代购食堂饭菜", "生病在宿舍，求带饭", "其他", 8.0),
        new DemandTemplate("考研数学辅导", "线代和概率论辅导", "学习辅导", 55.0),
        new DemandTemplate("Python数据分析指导", "机器学习作业需要帮助", "学习辅导", 70.0),
        new DemandTemplate("前端开发指导", "Vue项目问题求解", "学习辅导", 65.0),
        new DemandTemplate("笔记本电脑出售", "联想ThinkPad，办公学习用", "二手交易", 2500.0),
        new DemandTemplate("毕业甩卖小电器", "台灯、风扇、电水壶等", "二手交易", 50.0),
        new DemandTemplate("王者荣耀开黑", "找稳定队友上分", "活动组队", 0.0),
        new DemandTemplate("晨跑搭子", "每天早上6点半跑步", "活动组队", 0.0),
        new DemandTemplate("摄影外拍", "周末校园拍照", "活动组队", 0.0)
    );

    // 固定评价内容
    private static final List<String> REVIEW_CONTENTS = Arrays.asList(
        "非常满意，速度快态度好！",
        "很专业，问题都解决了",
        "非常好，强烈推荐！",
        "效率很高，五星好评",
        "靠谱的人，值得信赖",
        "沟通顺畅，合作愉快",
        "认真负责，值得推荐",
        "响应迅速，解决问题快",
        "非常专业的服务",
        "人很nice，必须好评",
        "有耐心，讲解清楚",
        "准时守信，很靠谱"
    );

    // 固定消息内容
    private static final List<String> MESSAGE_TEMPLATES = Arrays.asList(
        "你好！我看到你发布了需求，方便聊聊吗？",
        "请问这个需求还有效吗？",
        "我可以帮你做这个，什么时候方便？",
        "谢谢你的帮助！",
        "好的，我们到时候见",
        "没问题，交给我吧",
        "请问具体位置在哪里？",
        "好的，我已经到了",
        "辛苦了，非常感谢",
        "明天几点见面比较合适？",
        "价格可以再商量一下吗？",
        "可以的，这个价格我能接受",
        "合作愉快！",
        "任务已完成，请验收",
        "做得很棒，下次还找你"
    );

    @Override
    public void run(String... args) throws Exception {
        System.out.println("======== 开始初始化校园互助平台数据 ========");
        
    
        // 1. 获取所有用户
        List<User> allUsers = initAllUsers();
        
        if (allUsers.isEmpty()) {
            System.out.println("没有找到现有用户，请先确保用户已存在");
            return;
        }
        
        System.out.println("现有用户数量: " + allUsers.size());
        
        // 分离超级管理员和普通用户
        User superAdmin = allUsers.stream()
                .filter(User::isSuperAdmin)
                .findFirst()
                .orElse(null);
        
        List<User> normalUsers = allUsers.stream()
                .filter(u -> !u.isSuperAdmin())
                .collect(Collectors.toList());
        
        System.out.println("普通用户数量: " + normalUsers.size());
        
        // 2. 清空旧数据（保持外键约束顺序）
        System.out.println("正在清理旧数据...");
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        reviewRepository.deleteAll();
        orderRepository.deleteAll();
        demandRepository.deleteAll();
        System.out.println("旧数据清理完成");
        
        // 3. 为每个用户生成固定数量的需求
        List<Demand> allDemands = generateFixedDemands(normalUsers);
        System.out.println("生成需求 " + allDemands.size() + " 条");
        
        // 4. 生成订单和评价
        List<com.example.backend.entity.Order> allOrders = generateFixedOrdersAndReviews(allDemands, normalUsers);
        System.out.println("生成订单 " + allOrders.size() + " 条");
        System.out.println("生成评价 " + reviewRepository.count() + " 条");
        
        // 5. 更新用户信用分
        updateAllUsersScores(normalUsers);
        
        // 6. 生成对话和消息
        int[] convMsgCount = generateFixedConversationsAndMessages(normalUsers);
        System.out.println("生成对话 " + convMsgCount[0] + " 个，消息 " + convMsgCount[1] + " 条");
        
        printStatistics(normalUsers, allDemands, allOrders);

        System.out.println("======== 数据初始化完成 ========");
    }
    

    private List<User> initAllUsers() {
        List<User> allUsers = userRepository.findAll();
        if (allUsers.isEmpty()) {
            User superAdmin = initSuperAdmin();
            allUsers.add(superAdmin);
            List<User> normalUsers = initNormalUsers();
            allUsers.addAll(normalUsers);
        }
        return allUsers;
    }

     /**
     * 初始化超级管理员
     */
    private User initSuperAdmin() {
        boolean hasSuperAdmin = userRepository.findAll().stream()
                .anyMatch(User::isSuperAdmin);
        
        if (hasSuperAdmin) {
            System.out.println("超级管理员已存在，跳过初始化");
            return userRepository.findAll().stream()
                    .filter(User::isSuperAdmin)
                    .findFirst()
                    .get();
        }
        
        User rootUser = new User();
        rootUser.setName("校园互助平台管理员");
        rootUser.setPhone("123456");
        rootUser.setPassword(md5("123456"));
        rootUser.setScoreNum(0L);
        rootUser.setAverageScore(null);
        rootUser.setAdmin(true);
        rootUser.setSuperAdmin(true);        
        User saved = userRepository.save(rootUser);
        System.out.println("超级管理员初始化完成：手机号 123456，密码 123456");
        return saved;
    }


     /**
     * 初始化普通用户（模拟校园学生）
     */
    private List<User> initNormalUsers() {
        List<User> users = new ArrayList<>();
        
        // 学生用户数据 (姓名, 手机号, 密码)
        Object[][] studentData = {
            {"张三", "13800000001", "123456", "计算机科学与技术", "大三"},
            {"李四", "13800000002", "123456", "软件工程", "大二"},
            {"王芳", "13800000003", "123456", "信息管理", "大四"},
            {"赵雷", "13800000004", "123456", "网络工程", "研一"},
            {"陈晨", "13800000005", "123456", "人工智能", "大二"},
            {"刘阳", "13800000006", "123456", "数据科学", "大三"},
            {"周婷", "13800000007", "123456", "信息安全", "大四"},
            {"吴迪", "13800000008", "123456", "物联网工程", "大二"},
            {"郑爽", "13800000009", "123456", "智能科学", "研二"},
            {"林欣", "13800000010", "123456", "电子工程", "大三"},
            {"郭峰", "13800000011", "123456", "通信工程", "大二"},
            {"唐雅", "13800000012", "123456", "自动化", "大四"},
        };
        
        for (Object[] data : studentData) {
            String phone = (String) data[1];
            if (userRepository.existsByPhone(phone)) {
                continue;
            }
            
            User user = new User();
            user.setName((String) data[0]);
            user.setPhone(phone);
            user.setPassword(md5((String) data[2]));
            user.setScoreNum(0L);
            user.setAverageScore(null);
            user.setAdmin(false);
            user.setSuperAdmin(false);            
            users.add(userRepository.save(user));
        }
        
        System.out.println("初始化普通用户 " + users.size() + " 人");
        return users;
    }
    /**
     * 为每个用户生成固定数量的需求
     * 每个用户发布 3-6 条需求
     */
    private List<Demand> generateFixedDemands(List<User> users) {
        List<Demand> demands = new ArrayList<>();
        int demandIdCounter = 1;
        
        for (User user : users) {
            // 每个用户发布3-6条需求
            int demandCount = 3 + (user.getId().intValue() % 4); // 3,4,5,6
            
            for (int i = 0; i < demandCount; i++) {
                DemandTemplate template = DEMAND_TEMPLATES.get(
                    (demandIdCounter + i) % DEMAND_TEMPLATES.size()
                );
                
                Demand demand = new Demand();
                demand.setTitle(template.title);
                demand.setDescription(template.description);
                demand.setCategory(template.category);
                demand.setPublisherId(user.getId());
                
                // 固定地点循环
                demand.setLocation(LOCATIONS.get((demandIdCounter + i) % LOCATIONS.size()));
                demand.setReward(template.reward);
                demand.setDeadline(LocalDateTime.now().plusDays(7 + (i % 14)));
                
                // 状态分配：前2条已完成，其余活跃
                if (i < 2) {
                    demand.setStatus("COMPLETED");
                } else {
                    demand.setStatus(i % 2 == 0 ? "PENDING" : "ACCEPTED");
                }

                //随机添加图片（测试图片）
                if(i % 3 == 0 || i % 5 ==  0){
                    demand.setPictureUrls("/api/files/test.jpg");
                }
                
                // 创建时间：最近30天内
                demand.setCreatedAt(LocalDateTime.now().minusDays(demandIdCounter % 30));
                demand.setUpdatedAt(demand.getCreatedAt());
                
                Demand saved = demandRepository.save(demand);
                demands.add(saved);
                demandIdCounter++;
            }
        }
        
        return demands;
    }
    
    /**
     * 生成固定订单和评价
     * 确保每个用户都有作为接单者的订单和评价
     */
    private List<com.example.backend.entity.Order> generateFixedOrdersAndReviews(List<Demand> demands, List<User> users) {
        List<com.example.backend.entity.Order> orders = new ArrayList<>();
        int userCount = users.size();
        
        // 为每个已完成的需求创建订单
        List<Demand> completedDemands = demands.stream()
                .filter(d -> "COMPLETED".equals(d.getStatus()))
                .collect(Collectors.toList());
        
        int orderIdCounter = 1;
        
        for (Demand demand : completedDemands) {
            // 选择不同的接单者（不能是发布者自己）
            Long publisherId = demand.getPublisherId();
            User acceptor = findDifferentUser(users, publisherId, orderIdCounter);
            
            com.example.backend.entity.Order order = new com.example.backend.entity.Order();
            order.setDemandId(demand.getId());
            order.setPublisherId(publisherId);
            order.setAcceptorId(acceptor.getId());
            order.setStatus("COMPLETED");
            
            // 时间线
            LocalDateTime createdAt = demand.getCreatedAt().plusHours(orderIdCounter % 24);
            order.setCreatedAt(createdAt);
            LocalDateTime completedAt = createdAt.plusDays(1 + (orderIdCounter % 5));
            order.setCompletedAt(completedAt);
            order.setUpdatedAt(completedAt);
            
            String[] notes = {"好的，我来帮你", "没问题，保证完成任务", "收到，马上联系你", "OK，交给我吧"};
            order.setLatestRequesterNote(notes[orderIdCounter % notes.length]);
            
            com.example.backend.entity.Order savedOrder = orderRepository.save(order);
            orders.add(savedOrder);
            
            // 更新需求的orderId
            demand.setOrderId(savedOrder.getId());
            demandRepository.save(demand);
            
            // 发布者给接单者评价（固定好评）
            Review review = createReview(
                savedOrder.getId(),
                publisherId,
                acceptor.getId(),
                4 + (orderIdCounter % 2), // 4或5分
                REVIEW_CONTENTS.get(orderIdCounter % REVIEW_CONTENTS.size())
            );
            reviewRepository.save(review);
            savedOrder.setCommentId(review.getId());
            orderRepository.save(savedOrder);
            
            // 80%的概率双向评价（接单者给发布者评价）
            if (orderIdCounter % 5 != 0) {
                Review review2 = createReview(
                    savedOrder.getId(),
                    acceptor.getId(),
                    publisherId,
                    4 + ((orderIdCounter + 1) % 2),
                    REVIEW_CONTENTS.get((orderIdCounter + 1) % REVIEW_CONTENTS.size())
                );
                reviewRepository.save(review2);
            }
            
            orderIdCounter++;
        }
        
        // 确保每个用户都有至少2条作为接单者的完成订单
        Map<Long, Integer> acceptorOrderCount = new HashMap<>();
        for (User user : users) {
            acceptorOrderCount.put(user.getId(), 0);
        }
        
        for (com.example.backend.entity.Order order : orders) {
            acceptorOrderCount.merge(order.getAcceptorId(), 1, Integer::sum);
        }
        
        // 为缺少订单的用户补充订单
        for (User user : users) {
            int currentCount = acceptorOrderCount.getOrDefault(user.getId(), 0);
            int needed = Math.max(0, 2 - currentCount);
            
            for (int i = 0; i < needed; i++) {
                // 找该用户未完成的需求
                List<Demand> pendingDemands = demands.stream()
                        .filter(d -> !"COMPLETED".equals(d.getStatus()) 
                                && !d.getPublisherId().equals(user.getId()))
                        .collect(Collectors.toList());
                
                if (pendingDemands.isEmpty()) continue;
                
                Demand demand = pendingDemands.get(i % pendingDemands.size());
                
                com.example.backend.entity.Order order = new com.example.backend.entity.Order();
                order.setDemandId(demand.getId());
                order.setPublisherId(demand.getPublisherId());
                order.setAcceptorId(user.getId());
                order.setStatus("COMPLETED");
                
                LocalDateTime createdAt = LocalDateTime.now().minusDays(10 + i);
                order.setCreatedAt(createdAt);
                LocalDateTime completedAt = createdAt.plusDays(2);
                order.setCompletedAt(completedAt);
                order.setUpdatedAt(completedAt);
                order.setLatestRequesterNote("我来帮你完成这个任务");
                
                com.example.backend.entity.Order savedOrder = orderRepository.save(order);
                orders.add(savedOrder);
                
                demand.setStatus("COMPLETED");
                demand.setOrderId(savedOrder.getId());
                demandRepository.save(demand);
                
                // 添加评价
                Review review = createReview(
                    savedOrder.getId(),
                    demand.getPublisherId(),
                    user.getId(),
                    4,
                    "非常满意，谢谢！"
                );
                reviewRepository.save(review);
                savedOrder.setCommentId(review.getId());
                orderRepository.save(savedOrder);
            }
        }
        
        return orders;
    }
    
    /**
     * 创建评价对象
     */
    private Review createReview(Long orderId, Long reviewerId, Long reviewedId, int score, String content) {
        Review review = new Review();
        review.setOrderId(orderId);
        review.setReviewerId(reviewerId);
        review.setReviewedId(reviewedId);
        review.setScore(score);
        review.setContent(content);
        review.setCreatedAt(LocalDateTime.now().minusDays((int)(Math.random() * 10)));
        return review;
    }
    
    /**
     * 找一个不同的用户
     */
    private User findDifferentUser(List<User> users, Long excludeId, int seed) {
        List<User> candidates = users.stream()
                .filter(u -> !u.getId().equals(excludeId))
                .collect(Collectors.toList());
        
        if (candidates.isEmpty()) {
            return users.get(0);
        }
        
        return candidates.get(seed % candidates.size());
    }
    
    /**
     * 更新所有用户的信用分
     */
    private void updateAllUsersScores(List<User> users) {
        for (User user : users) {
            Double avgScore = reviewRepository.getAverageScoreForUser(user.getId());
            Long scoreCount = reviewRepository.getScoreCountForUser(user.getId());
            
            if (avgScore != null && scoreCount != null && scoreCount > 0) {
                userRepository.updateUserScore(user.getId(), avgScore, scoreCount);
                System.out.printf("用户 %s 信用分: %.1f (%d条评价)\n", 
                    user.getName(), avgScore, scoreCount);
            } else {
                // 没有评价的用户给一个默认评分
                userRepository.updateUserScore(user.getId(), 4.0, 1L);
                System.out.printf("用户 %s 无评价，设置默认信用分 4.0\n", user.getName());
            }
        }
    }
    
    /**
     * 生成固定对话和消息
     * 每个用户至少与其他2个用户有对话
     */
    private int[] generateFixedConversationsAndMessages(List<User> users) {
        int conversationCount = 0;
        int messageCount = 0;
        int userCount = users.size();
        
        // 确保每个用户至少与其他2个用户有对话
        for (int i = 0; i < userCount; i++) {
            User user1 = users.get(i);
            
            // 每个用户与后面2-3个用户建立对话
            int targetCount = Math.min(3, userCount - 1);
            
            for (int j = 1; j <= targetCount; j++) {
                int otherIndex = (i + j) % userCount;
                if (otherIndex == i) continue;
                
                User user2 = users.get(otherIndex);
                
                // 检查是否已存在对话
                if (conversationRepository.findConversationBetweenUsers(user1.getId(), user2.getId()).isPresent()) {
                    continue;
                }
                
                // 创建对话
                Conversation conversation = new Conversation(user1.getId(), user2.getId());
                conversation.setCreatedAt(LocalDateTime.now().minusDays(15 + i * 2));
                Conversation savedConv = conversationRepository.save(conversation);
                conversationCount++;
                
                // 生成固定数量消息（6-12条）
                int msgCount = 6 + ((i + j) % 7);
                LocalDateTime msgTime = savedConv.getCreatedAt();
                
                Long lastMessageId = null;
                LocalDateTime lastMessageTime = msgTime;
                
                for (int k = 0; k < msgCount; k++) {
                    Long senderId = (k % 2 == 0) ? user1.getId() : user2.getId();
                    Long receiverId = (senderId.equals(user1.getId())) ? user2.getId() : user1.getId();
                    
                    String content = MESSAGE_TEMPLATES.get(k % MESSAGE_TEMPLATES.size());
                    
                    Message message = new Message(senderId, receiverId, content, savedConv.getId());
                    message.setCreatedAt(msgTime.plusMinutes(k * 5 + k * 2));
                    message.setRead(k < msgCount - 1); // 最后一条未读
                    
                    Message savedMsg = messageRepository.save(message);
                    messageCount++;
                    lastMessageId = savedMsg.getId();
                    lastMessageTime = savedMsg.getCreatedAt();
                }
                
                // 更新最后消息
                if (lastMessageId != null) {
                    savedConv.setLastMessageId(lastMessageId);
                    savedConv.setLastMessageTime(lastMessageTime);
                    conversationRepository.save(savedConv);
                }
            }
        }
        
        return new int[]{conversationCount, messageCount};
    }
    
    /**
     * 打印统计信息
     */
    private void printStatistics(List<User> users, List<Demand> demands, List<com.example.backend.entity.Order> orders) {
        System.out.println("============================");
        System.out.println("最终统计信息：");
        System.out.println("  - 用户数：" + users.size());
        System.out.println("  - 总需求数：" + demands.size());
        System.out.println("  - 已完成需求：" + demands.stream().filter(d -> "COMPLETED".equals(d.getStatus())).count());
        System.out.println("  - 进行中需求：" + demands.stream().filter(d -> "PENDING".equals(d.getStatus())).count());
        System.out.println("  - 已接单需求：" + demands.stream().filter(d -> "ACCEPTED".equals(d.getStatus())).count());
        System.out.println("  - 订单数：" + orders.size());
        System.out.println("  - 评分数：" + reviewRepository.count());
        System.out.println("  - 对话数：" + conversationRepository.count());
        System.out.println("  - 消息数：" + messageRepository.count());
        
        // 打印每个用户的统计
        System.out.println("\n用户详细统计：");
        for (User user : users) {
            long demandCount = demandRepository.findByPublisherId(user.getId(), null).getTotalElements();
            long orderCount = orderRepository.findAllByUserId(user.getId(), null).getTotalElements();
            Long reviewCount = reviewRepository.getScoreCountForUser(user.getId());
            long convCount = conversationRepository.countByUserId(user.getId());
            long unreadCount = messageRepository.countUnreadMessagesByUserId(user.getId());
            
            System.out.printf("  - %s (ID:%d): 需求%d条, 订单%d个, 评价%d条, 对话%d个, 未读%d条, 信用分%.1f\n",
                    user.getName(), user.getId(),
                    demandCount, orderCount,
                    reviewCount != null ? reviewCount : 0, 
                    convCount, unreadCount,
                    user.getAverageScore() != null ? user.getAverageScore() : 0);
        }
        System.out.println("============================");
    }

    /**
     * MD5加密
     */
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
    
    /**
     * 需求模板内部类
     */
    private static class DemandTemplate {
        final String title;
        final String description;
        final String category;
        final double reward;
        
        DemandTemplate(String title, String description, String category, double reward) {
            this.title = title;
            this.description = description;
            this.category = category;
            this.reward = reward;
        }
    }
}