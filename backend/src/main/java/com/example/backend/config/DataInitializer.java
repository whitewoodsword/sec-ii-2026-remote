package com.example.backend.config;

import com.example.backend.entity.Conversation;
import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(1) // 确保优先执行
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已有超级管理员
        boolean hasSuperAdmin = userRepository.findAll().stream()
                .anyMatch(User::isSuperAdmin);
        
        System.out.println("========UserInitInfo========");
        
        User rootUser = null;
        if (!hasSuperAdmin) {
            rootUser = createRootUser();
            System.out.println("Root super admin user initialized successfully!");
        } else {
            rootUser = userRepository.findAll().stream()
                    .filter(User::isSuperAdmin)
                    .findFirst()
                    .orElse(null);
            System.out.println("Super admin already exists, skipping initialization.");
        }
        
        // 初始化普通测试用户（手机号1，密码1）
        User testUser = createTestUser();
        System.out.println("Test user initialized: phone=1, password=1");
        
        // 初始化root和testUser之间的对话和消息
        if (rootUser != null && testUser != null) {
            initializeConversationAndMessages(rootUser, testUser);
            System.out.println("Conversation and messages initialized between root and test user");
        }
        
        System.out.println("============================");
    }
    
    private User createRootUser() {
        User rootUser = new User();
        rootUser.setName("root");
        rootUser.setPhone("123456");
        rootUser.setPassword(md5("root"));
        rootUser.setScoreNum(0L);
        rootUser.setAverageScore(5.0);
        rootUser.setAdmin(true);
        rootUser.setSuperAdmin(true);
        rootUser.setAvatarPath(null);
        rootUser.setToken(null);
        
        return userRepository.save(rootUser);
    }
    
    private User createTestUser() {
        // 检查是否已存在手机号为1的用户
        if (userRepository.existsByPhone("1")) {
            return userRepository.findByPhone("1").orElse(null);
        }
        
        User testUser = new User();
        testUser.setName("测试用户");
        testUser.setPhone("1");
        testUser.setPassword(md5("1"));
        testUser.setScoreNum(0L);
        testUser.setAverageScore(5.0);
        testUser.setAdmin(false);
        testUser.setSuperAdmin(false);
        testUser.setAvatarPath(null);
        testUser.setToken(null);
        
        return userRepository.save(testUser);
    }
    
    private void initializeConversationAndMessages(User user1, User user2) {
        // 检查是否已存在对话
        Long user1Id = user1.getId();
        Long user2Id = user2.getId();
        
        // 确保user1Id < user2Id 或者直接查询
        Conversation existingConversation = null;
        List<Conversation> user1Conversations = conversationRepository.findAllByUserId(user1Id, org.springframework.data.domain.Pageable.unpaged());
        if (user1Conversations != null) {
            existingConversation = user1Conversations.stream()
                    .filter(c -> (c.getUser1Id().equals(user1Id) && c.getUser2Id().equals(user2Id)) ||
                                 (c.getUser1Id().equals(user2Id) && c.getUser2Id().equals(user1Id)))
                    .findFirst()
                    .orElse(null);
        }
        
        // 如果对话已存在且有消息，则不重复初始化
        if (existingConversation != null) {
            long messageCount = messageRepository.findByConversationIdOrderByCreatedAtAsc(existingConversation.getId()).size();
            if (messageCount > 0) {
                System.out.println("Conversation and messages already exist, skipping initialization.");
                return;
            }
        }
        
        // 创建对话
        Conversation conversation = new Conversation(user1Id, user2Id);
        conversation.setCreatedAt(LocalDateTime.now());
        Conversation savedConversation = conversationRepository.save(conversation);
        
        // 创建示例消息：user1 -> user2
        Message message1 = new Message(user1Id, user2Id, "你好！我是" + user1.getName() + "，欢迎使用校园互助平台！", savedConversation.getId());
        message1.setCreatedAt(LocalDateTime.now().minusDays(2));
        Message savedMessage1 = messageRepository.save(message1);
        
        // 创建示例消息：user2 -> user1
        Message message2 = new Message(user2Id, user1Id, "你好" + user1.getName() + "！很高兴认识你，这个平台看起来很不错。", savedConversation.getId());
        message2.setCreatedAt(LocalDateTime.now().minusDays(1).minusHours(12));
        Message savedMessage2 = messageRepository.save(message2);
        
        // 创建示例消息：user1 -> user2
        Message message3 = new Message(user1Id, user2Id, "是的！你可以在这里发布需求、接单赚钱、结识朋友。有什么问题随时问我！", savedConversation.getId());
        message3.setCreatedAt(LocalDateTime.now().minusHours(5));
        Message savedMessage3 = messageRepository.save(message3);
        
        // 创建示例消息：user2 -> user1（未读消息）
        Message message4 = new Message(user2Id, user1Id, "太好了！我有个快递需要代取，应该怎么操作呢？", savedConversation.getId());
        message4.setCreatedAt(LocalDateTime.now().minusHours(1));
        message4.setRead(false);
        Message savedMessage4 = messageRepository.save(message4);
        
        // 更新对话的最后消息信息
        conversation.setLastMessageId(savedMessage4.getId());
        conversation.setLastMessageTime(savedMessage4.getCreatedAt());
        conversationRepository.save(conversation);
        
        System.out.println("Created conversation (ID: " + savedConversation.getId() + ") with 4 messages");
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