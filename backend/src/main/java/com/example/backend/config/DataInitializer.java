package com.example.backend.config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    private Random random = new Random();

    // 定义校园常见分类
    private static final String[] CATEGORIES = {"快递代取", "学习辅导", "二手交易", "活动组队", "其他"};
    
    // 定义校园地点
    private static final String[] LOCATIONS = {
        "南区宿舍", "北区宿舍", "图书馆", "第一食堂", "第二食堂", 
        "教学楼A座", "教学楼B座", "体育馆", "游泳馆", "学生活动中心"
    };
    
    // 定义头像路径
    private static final String[] AVATAR_PATHS = {
        "/avatars/default1.png", "/avatars/default2.png", "/avatars/default3.png",
        "/avatars/default4.png", "/avatars/default5.png"
    };

    @Override
    public void run(String... args) throws Exception {
        System.out.println("======== 开始初始化校园互助平台数据 ========");
        
        // 1. 初始化超级管理员
        User rootUser = initSuperAdmin();
        
        // 2. 初始化普通测试用户
        List<User> normalUsers = initNormalUsers();
        
        // 3. 初始化需求数据
        List<Demand> demands = initDemands(rootUser, normalUsers);
        
        // 4. 初始化订单数据
        List<com.example.backend.entity.Order> orders = initOrders(demands, normalUsers);
        
        // 5. 初始化评价数据
        initReviews(orders, normalUsers);
        
        // 6. 初始化对话和消息
        initConversationsAndMessages(rootUser, normalUsers);
        
        System.out.println("======== 数据初始化完成 ========");
        System.out.println("统计信息：");
        System.out.println("  - 用户数：" + (normalUsers.size() + 1));
        System.out.println("  - 需求数：" + demands.size());
        System.out.println("  - 订单数：" + orders.size());
        System.out.println("  - 评分数：" + reviewRepository.count());
        System.out.println("============================");
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
        rootUser.setAverageScore(5.0);
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
        
        // 添加测试用户（手机号1）
        if (!userRepository.existsByPhone("1")) {
            User testUser = new User();
            testUser.setName("测试用户");
            testUser.setPhone("1");
            testUser.setPassword(md5("1"));
            testUser.setScoreNum(0L);
            testUser.setAverageScore(null);
            testUser.setAdmin(false);
            testUser.setSuperAdmin(false);
            users.add(userRepository.save(testUser));
            System.out.println("测试用户初始化：手机号 1，密码 1");
        } else {
            userRepository.findByPhone("1").ifPresent(users::add);
        }
        
        System.out.println("初始化普通用户 " + users.size() + " 人");
        return users;
    }
    
    /**
     * 初始化需求数据
     */
    private List<Demand> initDemands(User rootUser, List<User> users) {
        List<Demand> demands = new ArrayList<>();
        
        // 需求数据数组
        Object[][] demandsData = {
            // 快递代取类
            {"急！帮我取个快递", "快递在菜鸟驿站，比较大件，需要两个人帮忙搬到宿舍", "快递代取", "南区菜鸟驿站", 2, 15.0},
            {"帮忙取快递", "顺丰快递，小件，送到北区宿舍楼下", "快递代取", "北区快递点", 1, 8.0},
            {"求代取快递", "三个快递，在邮政收发室，送到图书馆", "快递代取", "邮政收发室", 1, 12.0},
            
            // 学习辅导类
            {"考研数学辅导", "需要辅导高等数学，主要是微积分部分，周末有空", "学习辅导", "图书馆自习室", 3, 50.0},
            {"Java编程作业辅导", "Java课程设计不会做，需要大神指导，在线辅导也可以", "学习辅导", "教学楼A座", 2, 40.0},
            {"英语四六级辅导", "英语基础薄弱，希望找人一起练习听力和阅读", "学习辅导", "学生活动中心", 4, 35.0},
            {"大作业代码调试", "Web项目有个bug一直找不到原因，有偿求助", "学习辅导", "线上", 1, 30.0},
            {"期末复习资料分享", "求计算机网络期末复习资料和往年试题", "学习辅导", "图书馆", 1, 10.0},
            
            // 二手交易类
            {"出二手电动车", "九成新电动车，骑了半年，续航30km，带发票", "二手交易", "南区宿舍", 0, 800.0},
            {"二手教材出售", "大一到大三计算机专业教材，几乎全新，打包优惠", "二手交易", "北区宿舍", 0, 150.0},
            {"出吉他", "练习吉他，买来没用几次，适合新手", "二手交易", "学生活动中心", 1, 200.0},
            {"求购二手自行车", "预算200左右，要求能正常骑行", "二手交易", "全校", 2, 200.0},
            {"毕业甩卖小电器", "台灯、风扇、电水壶等，价格便宜", "二手交易", "南区宿舍", 3, 50.0},
            
            // 活动组队类
            {"周末羽毛球约球", "本人菜鸟，想找人一起打羽毛球锻炼身体", "活动组队", "体育馆", 2, 0.0},
            {"组队参加校园马拉松", "5km组，找一起跑步的小伙伴", "活动组队", "体育场", 3, 0.0},
            {"读书会招募", "每周三晚上一起读书分享，这周读《活着》", "活动组队", "图书馆研讨室", 1, 0.0},
            {"王者荣耀开黑", "找稳定队友上分，主玩中路辅助", "活动组队", "线上", 2, 0.0},
            {"摄影爱好者外拍", "周末去校园拍秋景，找摄影同好一起", "活动组队", "校园内", 1, 0.0},
            
            // 其他类
            {"帮忙占座", "明天上午图书馆三楼靠窗位置，有偿", "其他", "图书馆", 1, 5.0},
            {"寻物启事", "丢失蓝色水杯一个，捡到请联系", "其他", "教学楼", 0, 0.0},
            {"上门喂猫", "周末出门两天，需要帮忙喂一下小猫", "其他", "北区宿舍", 1, 30.0},
        };
        
        List<User> allUsers = new ArrayList<>(users);
        allUsers.add(rootUser);
        
        for (int i = 0; i < demandsData.length; i++) {
            Object[] data = demandsData[i];
            Long publisherId = allUsers.get(random.nextInt(allUsers.size())).getId();
            
            // 设置截止时间（3天到30天不等）
            LocalDateTime deadline = LocalDateTime.now().plusDays(3 + random.nextInt(27));
            
            // 判断是否有报酬
            Double reward = (Double) data[5];
            
            Demand demand = new Demand();
            demand.setTitle((String) data[0]);
            demand.setDescription((String) data[1]);
            demand.setCategory((String) data[2]);
            demand.setPublisherId(publisherId);
            demand.setLocation((String) data[3]);
            demand.setDeadline(deadline);
            demand.setReward(reward);
            
            // 设置状态（大部分为PENDING，少量为其他状态）
            String status;
            int statusRand = random.nextInt(10);
            if (statusRand < 7) {
                status = "PENDING";
            } else if (statusRand < 9) {
                status = "ACCEPTED";
            } else {
                status = "COMPLETED";
            }
            demand.setStatus(status);
            
            demand.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(30)));
            demand.setUpdatedAt(demand.getCreatedAt());
            
            // 随机添加图片
            if (random.nextBoolean()) {
                demand.setPictureUrls("/images/demand_" + (i + 1) + ".jpg");
            }
            
            demands.add(demandRepository.save(demand));
        }
        
        System.out.println("初始化需求 " + demands.size() + " 条");
        return demands;
    }
    
    /**
     * 初始化订单数据（基于已接受的需求）
     */
    private List<com.example.backend.entity.Order> initOrders(List<Demand> demands, List<User> users) {
        List<com.example.backend.entity.Order> orders = new ArrayList<>();
        List<User> allUsers = new ArrayList<>(users);
        
        for (Demand demand : demands) {
            // 只为 ACCEPTED 或 COMPLETED 状态的需求创建订单
            if (!"ACCEPTED".equals(demand.getStatus()) && !"COMPLETED".equals(demand.getStatus())) {
                continue;
            }
            
            // 找接单者（不能是发布者自己）
            Long acceptorId;
            do {
                acceptorId = allUsers.get(random.nextInt(allUsers.size())).getId();
            } while (acceptorId.equals(demand.getPublisherId()));
            
            com.example.backend.entity.Order order = new com.example.backend.entity.Order();
            order.setDemandId(demand.getId());
            order.setPublisherId(demand.getPublisherId());
            order.setAcceptorId(acceptorId);
            
            // 订单状态
            String orderStatus = "ACCEPTED".equals(demand.getStatus()) ? "ACCEPTED" : "COMPLETED";
            order.setStatus(orderStatus);
            
            order.setCreatedAt(demand.getCreatedAt().plusHours(random.nextInt(24)));
            order.setUpdatedAt(order.getCreatedAt());
            
            if ("COMPLETED".equals(orderStatus)) {
                order.setCompletedAt(order.getCreatedAt().plusDays(random.nextInt(7) + 1));
                order.setUpdatedAt(order.getCompletedAt());
            }
            
            // 添加备注
            String[] notes = {"好的，我来帮你", "请问具体什么时候方便", "没问题，保证完成任务"};
            order.setLatestRequesterNote(notes[random.nextInt(notes.length)]);
            
            com.example.backend.entity.Order savedOrder = orderRepository.save(order);
            orders.add(savedOrder);
            
            // 更新需求的orderId
            demand.setOrderId(savedOrder.getId());
            demandRepository.save(demand);
        }
        
        System.out.println("初始化订单 " + orders.size() + " 条");
        return orders;
    }
    
    /**
     * 初始化评价数据
     */
    private void initReviews(List<com.example.backend.entity.Order> orders, List<com.example.backend.entity.User> users) {
        List<com.example.backend.entity.Review> reviews = new ArrayList<>();
        
        for (com.example.backend.entity.Order order : orders) {
            // 只为已完成订单创建评价
            if (!"COMPLETED".equals(order.getStatus())) {
                continue;
            }
            
            // 随机决定是否已有评价
            if (random.nextDouble() > 0.7) { // 70%的完成订单有评价
                continue;
            }
            
            Demand demand = demandRepository.findById(order.getDemandId()).orElse(null);
            if (demand == null) continue;
            
            // 创建评价（发布者评价接单者）
            int score = 3 + random.nextInt(3); // 3-5分
            String[] reviewContents = {
                "非常满意，速度快态度好！",
                "还不错，希望下次还能合作",
                "很专业，问题都解决了",
                "非常好，强烈推荐！",
                "一般般，还有提升空间",
                "很棒的小伙伴，给力！",
                "感谢帮忙，下次还找你"
            };
            
            Review review1 = new Review();
            review1.setOrderId(order.getId());
            review1.setReviewerId(order.getPublisherId());
            review1.setReviewedId(order.getAcceptorId());
            review1.setScore(score);
            review1.setContent(reviewContents[random.nextInt(reviewContents.length)]);
            review1.setCreatedAt(order.getCompletedAt().plusHours(random.nextInt(48)));
            
            Review savedReview = reviewRepository.save(review1);
            reviews.add(savedReview);
            
            // 更新订单的commentId
            order.setCommentId(savedReview.getId());
            orderRepository.save(order);
            
            // 更新用户评分
            updateUserAverageScore(order.getAcceptorId());
            
            // 随机决定接单者是否也评价发布者
            if (random.nextDouble() > 0.5) {
                int score2 = 3 + random.nextInt(3);
                Review review2 = new Review();
                review2.setOrderId(order.getId());
                review2.setReviewerId(order.getAcceptorId());
                review2.setReviewedId(order.getPublisherId());
                review2.setScore(score2);
                review2.setContent(reviewContents[random.nextInt(reviewContents.length)]);
                review2.setCreatedAt(review1.getCreatedAt().plusHours(random.nextInt(24)));
                reviewRepository.save(review2);
                
                updateUserAverageScore(order.getPublisherId());
            }
        }
        
        System.out.println("初始化评价 " + reviews.size() + " 条");
    }
    
    /**
     * 更新用户的平均评分
     */
    private void updateUserAverageScore(Long userId) {
        Double avgScore = reviewRepository.getAverageScoreForUser(userId);
        Long scoreCount = reviewRepository.getScoreCountForUser(userId);
        
        if (avgScore != null && scoreCount != null) {
            userRepository.updateUserScore(userId, avgScore, scoreCount);
        }
    }
    
    /**
     * 初始化对话和消息
     */
    private void initConversationsAndMessages(User rootUser, List<User> users) {
        List<User> allUsers = new ArrayList<>(users);
        allUsers.add(rootUser);
        
        int conversationCount = 0;
        int messageCount = 0;
        
        // 为每对用户创建一些对话（限制数量避免过多）
        for (int i = 0; i < allUsers.size() && i < 8; i++) {
            for (int j = i + 1; j < allUsers.size() && j < 8; j++) {
                User user1 = allUsers.get(i);
                User user2 = allUsers.get(j);
                
                // 检查是否已存在对话
                if (conversationRepository.findConversationBetweenUsers(user1.getId(), user2.getId()).isPresent()) {
                    continue;
                }
                
                // 随机决定是否创建对话（50%概率）
                if (random.nextDouble() > 0.5) {
                    continue;
                }
                
                // 创建对话
                Conversation conversation = new Conversation(user1.getId(), user2.getId());
                conversation.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(20)));
                Conversation savedConv = conversationRepository.save(conversation);
                conversationCount++;
                
                // 创建消息
                int messageCountForConv = 2 + random.nextInt(8);
                LocalDateTime msgTime = savedConv.getCreatedAt();
                
                for (int k = 0; k < messageCountForConv; k++) {
                    Long senderId = (k % 2 == 0) ? user1.getId() : user2.getId();
                    Long receiverId = (senderId.equals(user1.getId())) ? user2.getId() : user1.getId();
                    
                    String[] messages = {
                        "你好！我看到你发布了需求，方便聊聊吗？",
                        "请问这个需求还有效吗？",
                        "我可以帮你做这个，什么时候方便？",
                        "谢谢你的帮助！",
                        "不客气，有问题随时联系",
                        "好的，我们到时候见",
                        "抱歉，我刚看到消息",
                        "没问题，交给我吧",
                        "请问具体位置在哪里？",
                        "好的，我已经到了"
                    };
                    
                    Message message = new Message(senderId, receiverId, 
                        messages[random.nextInt(messages.length)], savedConv.getId());
                    message.setCreatedAt(msgTime.plusMinutes(k * 5 + random.nextInt(60)));
                    message.setRead(random.nextBoolean());
                    
                    messageRepository.save(message);
                    messageCount++;
                }
                
                // 更新最后一条消息
                messageRepository.findByConversationIdOrderByCreatedAtAsc(savedConv.getId())
                    .stream().reduce((first, second) -> second)
                    .ifPresent(lastMsg -> {
                        savedConv.setLastMessageId(lastMsg.getId());
                        savedConv.setLastMessageTime(lastMsg.getCreatedAt());
                        conversationRepository.save(savedConv);
                    });
            }
        }
        
        System.out.println("初始化对话 " + conversationCount + " 个，消息 " + messageCount + " 条");
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