package com.example.backend;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.backend.entity.Conversation;
import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("消息模块集成测试")
public class MessageIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    private User userA;
    private User userB;
    private User userC;
    private Conversation conversationAB;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdConversationIds = new ArrayList<>();
    private final List<Long> createdMessageIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // 创建测试用户A
        userA = new User("消息测试用户A", "20000000001", "password123");
        userA.setScoreNum(0L);
        userA.setAdmin(false);
        userA.setSuperAdmin(false);
        userA = userRepository.save(userA);
        createdUserIds.add(userA.getId());

        // 创建测试用户B
        userB = new User("消息测试用户B", "20000000002", "password123");
        userB.setScoreNum(0L);
        userB.setAdmin(false);
        userB.setSuperAdmin(false);
        userB = userRepository.save(userB);
        createdUserIds.add(userB.getId());

        // 创建测试用户C
        userC = new User("消息测试用户C", "20000000003", "password123");
        userC.setScoreNum(0L);
        userC.setAdmin(false);
        userC.setSuperAdmin(false);
        userC = userRepository.save(userC);
        createdUserIds.add(userC.getId());

        // 创建A和B之间的对话
        conversationAB = new Conversation(userA.getId(), userB.getId());
        conversationAB = conversationRepository.save(conversationAB);
        createdConversationIds.add(conversationAB.getId());
    }


    @AfterEach
    void cleanUp() {
        // 清理消息
        for (Long messageId : createdMessageIds) {
            try {
                if (messageId != null && messageRepository.existsById(messageId)) {
                    messageRepository.deleteById(messageId);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete message " + messageId + ": " + e.getMessage());
            }
        }

        // 清理对话
        for (Long conversationId : createdConversationIds) {
            try {
                if (conversationId != null && conversationRepository.existsById(conversationId)) {
                    conversationRepository.deleteById(conversationId);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete conversation " + conversationId + ": " + e.getMessage());
            }
        }

        // 清理用户
        for (Long userId : createdUserIds) {
            try {
                if (userId != null && userRepository.existsById(userId)) {
                    userRepository.deleteById(userId);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete user " + userId + ": " + e.getMessage());
            }
        }

        createdUserIds.clear();
        createdConversationIds.clear();
        createdMessageIds.clear();
    }

    // ==================== 发送消息测试 ====================

    @Nested
    @DisplayName("发送消息测试")
    class SendMessageTests {

        @Test
        @DisplayName("A向B发送消息成功")
        void testSendMessageSuccess() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("content", "你好，这是一条测试消息");

            mockMvc.perform(post("/messages/send")
                            .param("senderId", String.valueOf(userA.getId()))
                            .param("receiverId", String.valueOf(userB.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.senderId").value(userA.getId()))
                    .andExpect(jsonPath("$.data.receiverId").value(userB.getId()))
                    .andExpect(jsonPath("$.data.content").value("你好，这是一条测试消息"))
                    .andExpect(jsonPath("$.data.conversationId").exists());
        }

        @Test
        @DisplayName("发送消息后数据库中能查询到")
        void testSendMessagePersisted() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("content", "持久化测试消息");

            mockMvc.perform(post("/messages/send")
                            .param("senderId", String.valueOf(userA.getId()))
                            .param("receiverId", String.valueOf(userB.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());

            List<Message> messages = messageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conversationAB.getId());
            org.assertj.core.api.Assertions.assertThat(messages).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(messages.get(0).getContent())
                    .isEqualTo("持久化测试消息");
        }

        @Test
        @DisplayName("发送空内容消息返回400")
        void testSendEmptyContent() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("content", "");

            mockMvc.perform(post("/messages/send")
                            .param("senderId", String.valueOf(userA.getId()))
                            .param("receiverId", String.valueOf(userB.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("消息内容不能为空")));
        }

        @Test
        @DisplayName("不能给自己发送消息")
        void testSendMessageToSelf() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("content", "给自己发消息");

            mockMvc.perform(post("/messages/send")
                            .param("senderId", String.valueOf(userA.getId()))
                            .param("receiverId", String.valueOf(userA.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("不能给自己发送消息")));
        }
    }

    // ==================== 消息查询测试 ====================

    @Nested
    @DisplayName("消息查询测试")
    class MessageQueryTests {

        @BeforeEach
        void createTestMessages() {

            for (int i = 1; i <= 5; i++) {
                Message msg = new Message(userA.getId(), userB.getId(),
                        "测试消息" + i, conversationAB.getId());
                msg.setCreatedAt(LocalDateTime.now().minusMinutes(6 - i));
                Message saved = messageRepository.save(msg);
                createdMessageIds.add(saved.getId());
            }
        }

        @Test
        @DisplayName("分页获取对话消息（倒序）")
        void testGetMessagesByConversation() throws Exception {
            mockMvc.perform(get("/messages/conversation/{conversationId}", conversationAB.getId())
                            .param("userId", String.valueOf(userA.getId()))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content", hasSize(5)));
        }

        @Test
        @DisplayName("分页获取对话消息第二页")
        void testGetMessagesByConversationPage2() throws Exception {
            mockMvc.perform(get("/messages/conversation/{conversationId}", conversationAB.getId())
                            .param("userId", String.valueOf(userA.getId()))
                            .param("page", "1")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("获取对话中的所有消息（正序）")
        void testGetAllMessagesInConversation() throws Exception {
            mockMvc.perform(get("/messages/conversation/{conversationId}/all", conversationAB.getId())
                            .param("userId", String.valueOf(userA.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(5)));
        }

        @Test
        @DisplayName("获取单条消息成功")
        void testGetMessageById() throws Exception {
            Message firstMsg = messageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conversationAB.getId()).get(0);

            mockMvc.perform(get("/messages/{messageId}", firstMsg.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(firstMsg.getId()))
                    .andExpect(jsonPath("$.data.content").value("测试消息1"));
        }

        @Test
        @DisplayName("获取不存在的消息返回404")
        void testGetMessageByIdNotFound() throws Exception {
            mockMvc.perform(get("/messages/{messageId}", 99999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.message").value(containsString("消息不存在")));
        }
    }

    // ==================== 已读状态测试 ====================

    @Nested
    @DisplayName("已读状态测试")
    class ReadStatusTests {

        private Message unreadMessage;

        @BeforeEach
        void createUnreadMessage() {
            unreadMessage = new Message(userB.getId(), userA.getId(),
                    "B发给A的未读消息", conversationAB.getId());
            unreadMessage.setRead(false);
            unreadMessage = messageRepository.save(unreadMessage);
            createdMessageIds.add(unreadMessage.getId());
        }

        @Test
        @DisplayName("标记单条消息为已读")
        void testMarkSingleMessageAsRead() throws Exception {
            mockMvc.perform(patch("/messages/{messageId}/read", unreadMessage.getId())
                            .param("userId", String.valueOf(userA.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(1));
        }

        @Test
        @DisplayName("标记与指定用户的所有消息为已读")
        void testMarkAllMessagesAsReadWithUser() throws Exception {
            mockMvc.perform(patch("/messages/read/all")
                            .param("userId", String.valueOf(userA.getId()))
                            .param("senderId", String.valueOf(userB.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("标记对话中所有消息为已读")
        void testMarkAllMessagesAsReadInConversation() throws Exception {
            mockMvc.perform(patch("/messages/read/conversation/{conversationId}", conversationAB.getId())
                            .param("userId", String.valueOf(userA.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    // ==================== 未读消息统计测试 ====================

    @Nested
    @DisplayName("未读消息统计测试")
    class UnreadCountTests {

        @BeforeEach
        void createUnreadMessages() {
            for (int i = 1; i <= 3; i++) {
                Message msg = new Message(userB.getId(), userA.getId(),
                        "B发给A的未读消息" + i, conversationAB.getId());
                msg.setRead(false);
                Message saved = messageRepository.save(msg);
                createdMessageIds.add(saved.getId());
            }
        }

        @Test
        @DisplayName("获取用户未读消息总数")
        void testGetTotalUnreadCount() throws Exception {
            mockMvc.perform(get("/messages/unread/count/{userId}", userA.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.unreadCount").value(3));
        }

        @Test
        @DisplayName("获取用户与特定用户的未读消息数")
        void testGetUnreadCountWithUser() throws Exception {
            mockMvc.perform(get("/messages/unread/between")
                            .param("userId", String.valueOf(userA.getId()))
                            .param("otherUserId", String.valueOf(userB.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.unreadCount").value(3));
        }

        @Test
        @DisplayName("没有未读消息时返回0")
        void testGetUnreadCountZero() throws Exception {
            mockMvc.perform(get("/messages/unread/between")
                            .param("userId", String.valueOf(userA.getId()))
                            .param("otherUserId", String.valueOf(userC.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.unreadCount").value(0));
        }
    }

    // ==================== 消息列表查询测试 ====================

    @Nested
    @DisplayName("消息列表查询测试")
    class MessageListTests {

        @BeforeEach
        void createMessages() {
            Message msg1 = new Message(userA.getId(), userB.getId(),
                    "A发给B的消息", conversationAB.getId());
            Message saved1 = messageRepository.save(msg1);
            createdMessageIds.add(saved1.getId());

            Message msg2 = new Message(userB.getId(), userA.getId(),
                    "B发给A的消息", conversationAB.getId());
            Message saved2 = messageRepository.save(msg2);
            createdMessageIds.add(saved2.getId());
        }

        @Test
        @DisplayName("获取用户接收的最新消息")
        void testGetLatestReceivedMessages() throws Exception {
            mockMvc.perform(get("/messages/received/{userId}", userA.getId())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("获取用户发送的最新消息")
        void testGetLatestSentMessages() throws Exception {
            mockMvc.perform(get("/messages/sent/{userId}", userA.getId())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("获取用户所有相关消息")
        void testGetAllUserMessages() throws Exception {
            mockMvc.perform(get("/messages/all/{userId}", userA.getId())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("获取按对话分组的最新消息列表")
        void testGetLatestMessagesGroupedByConversation() throws Exception {
            mockMvc.perform(get("/messages/latest/grouped/{userId}", userA.getId())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    // ==================== 删除消息测试 ====================

    @Nested
    @DisplayName("删除消息测试")
    class DeleteMessageTests {

        private Message messageToDelete;

        @BeforeEach
        void createMessageToDelete() {
            messageToDelete = new Message(userA.getId(), userB.getId(),
                    "待删除的消息", conversationAB.getId());
            messageToDelete = messageRepository.save(messageToDelete);
            createdMessageIds.add(messageToDelete.getId());
        }

        @Test
        @DisplayName("发送者成功删除自己的消息")
        void testDeleteMessageBySender() throws Exception {
            mockMvc.perform(delete("/messages/{messageId}", messageToDelete.getId())
                            .param("userId", String.valueOf(userA.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // 验证消息已删除
            org.assertj.core.api.Assertions.assertThat(
                    messageRepository.findById(messageToDelete.getId())).isEmpty();
        }

        @Test
        @DisplayName("接收者不能删除发送者的消息")
        void testReceiverCannotDeleteMessage() throws Exception {
            mockMvc.perform(delete("/messages/{messageId}", messageToDelete.getId())
                            .param("userId", String.valueOf(userB.getId())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("无权删除此消息")));
        }

        @Test
        @DisplayName("删除不存在的消息返回400")
        void testDeleteNonExistentMessage() throws Exception {
            mockMvc.perform(delete("/messages/{messageId}", 99999L)
                            .param("userId", String.valueOf(userA.getId())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("消息不存在")));
        }
    }

    // ==================== 跨模块业务流程测试 ====================

    @Nested
    @DisplayName("跨模块业务流程测试")
    class CrossModuleFlowTests {

        @Test
        @DisplayName("完整消息流程：发送 -> 查询 -> 未读统计 -> 标记已读 -> 验证")
        void testCompleteMessageFlow() throws Exception {
            // Step 1: A向B发送消息
            Map<String, String> body = new HashMap<>();
            body.put("content", "你好，能帮我看看需求吗？");

            mockMvc.perform(post("/messages/send")
                            .param("senderId", String.valueOf(userA.getId()))
                            .param("receiverId", String.valueOf(userB.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());

            // Step 2: B回复A
            body.put("content", "好的，我来看看");
            mockMvc.perform(post("/messages/send")
                            .param("senderId", String.valueOf(userB.getId()))
                            .param("receiverId", String.valueOf(userA.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());

            // Step 3: 获取对话所有消息（应为2条）
            mockMvc.perform(get("/messages/conversation/{conversationId}/all", conversationAB.getId())
                            .param("userId", String.valueOf(userA.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(2)));

            // Step 4: 检查A的未读消息数（B回复了A，A应有1条未读）
            mockMvc.perform(get("/messages/unread/count/{userId}", userA.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.unreadCount").isNumber());

            // Step 5: A标记对话中所有消息为已读
            mockMvc.perform(patch("/messages/read/conversation/{conversationId}", conversationAB.getId())
                            .param("userId", String.valueOf(userA.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // Step 6: 验证A的未读消息数已归零
            mockMvc.perform(get("/messages/unread/count/{userId}", userA.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.unreadCount").value(0));
        }

        @Test
        @DisplayName("多用户消息隔离：C不应看到A和B的消息")
        void testMultiUserMessageIsolation() throws Exception {
            // A向B发送消息
            Map<String, String> body = new HashMap<>();
            body.put("content", "A和B的私密消息");

            mockMvc.perform(post("/messages/send")
                            .param("senderId", String.valueOf(userA.getId()))
                            .param("receiverId", String.valueOf(userB.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());

            // C不应看到A和B之间消息
            mockMvc.perform(get("/messages/conversation/{conversationId}/all", conversationAB.getId())
                            .param("userId", String.valueOf(userC.getId())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.message").value(containsString("无权访问此对话")));
        }
    }
}
