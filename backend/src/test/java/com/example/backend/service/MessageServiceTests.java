package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.backend.entity.Conversation;
import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.MessageRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("消息服务单元测试")
class MessageServiceTests {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private ConversationRepository conversationRepository; 

    @Mock
    private UserService userService;

    @InjectMocks
    private MessageService messageService;

    private User testUser1;
    private User testUser2;
    private Message testMessage;
    private Conversation testConversation;
    private final Long TEST_USER_ID_1 = 100L;
    private final Long TEST_USER_ID_2 = 200L;
    private final Long TEST_CONVERSATION_ID = 1000L;
    private final Long TEST_MESSAGE_ID = 5000L;
    private final String TEST_CONTENT = "你好，这是一条测试消息";

    @BeforeEach
    void setUp() {
        testUser1 = new User("用户A", "13800138000", "password");
        testUser1.setId(TEST_USER_ID_1);
        testUser1.setAvatarPath("/avatar/a.jpg");

        testUser2 = new User("用户B", "13900139000", "password");
        testUser2.setId(TEST_USER_ID_2);
        testUser2.setAvatarPath("/avatar/b.jpg");

        testConversation = new Conversation();
        testConversation.setId(TEST_CONVERSATION_ID);
        testConversation.setUser1Id(TEST_USER_ID_1);
        testConversation.setUser2Id(TEST_USER_ID_2);
        testConversation.setLastMessageId(null);
        testConversation.setLastMessageTime(LocalDateTime.now());
        testConversation.setCreatedAt(LocalDateTime.now());

        testMessage = new Message(TEST_USER_ID_1, TEST_USER_ID_2, TEST_CONTENT, TEST_CONVERSATION_ID);
        testMessage.setId(TEST_MESSAGE_ID);
        testMessage.setRead(false);
        testMessage.setCreatedAt(LocalDateTime.now());
    }

    // ==================== 发送消息测试 ====================

    @Nested
    @DisplayName("发送消息测试")
    class SendMessageTests {

        @Test
        @DisplayName("成功发送消息")
        void testSendMessageSuccess() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_2))
                .thenReturn(testConversation);
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
            doNothing().when(conversationService).updateLastMessage(TEST_CONVERSATION_ID, TEST_MESSAGE_ID);

            // When
            Message result = messageService.sendMessage(TEST_USER_ID_1, TEST_USER_ID_2, TEST_CONTENT);

            // Then
            assertNotNull(result);
            assertEquals(TEST_USER_ID_1, result.getSenderId());
            assertEquals(TEST_USER_ID_2, result.getReceiverId());
            assertEquals(TEST_CONTENT, result.getContent());
            assertEquals(TEST_CONVERSATION_ID, result.getConversationId());
            assertFalse(result.isRead());
            assertNotNull(result.getCreatedAt());

            verify(messageRepository).save(any(Message.class));
            verify(conversationService).updateLastMessage(TEST_CONVERSATION_ID, TEST_MESSAGE_ID);
        }

        @Test
        @DisplayName("发送消息时发送者不存在应抛出异常")
        void testSendMessageSenderNotFound() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenThrow(new RuntimeException("用户不存在"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageService.sendMessage(TEST_USER_ID_1, TEST_USER_ID_2, TEST_CONTENT));
            assertEquals("用户不存在", exception.getMessage());

            verify(messageRepository, never()).save(any());
            verify(conversationService, never()).updateLastMessage(any(), any());
        }

        @Test
        @DisplayName("发送消息时接收者不存在应抛出异常")
        void testSendMessageReceiverNotFound() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenThrow(new RuntimeException("用户不存在"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageService.sendMessage(TEST_USER_ID_1, TEST_USER_ID_2, TEST_CONTENT));
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("不能给自己发送消息")
        void testSendMessageToSelf() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageService.sendMessage(TEST_USER_ID_1, TEST_USER_ID_1, TEST_CONTENT));
            assertEquals("不能给自己发送消息", exception.getMessage());

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("发送空内容消息")
        void testSendEmptyContentMessage() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_2))
                .thenReturn(testConversation);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message saved = invocation.getArgument(0);
                saved.setId(TEST_MESSAGE_ID);
                return saved;
            });
            doNothing().when(conversationService).updateLastMessage(TEST_CONVERSATION_ID, TEST_MESSAGE_ID);

            // When
            Message result = messageService.sendMessage(TEST_USER_ID_1, TEST_USER_ID_2, "");

            // Then
            assertNotNull(result);
            assertEquals("", result.getContent());
        }

        @Test
        @DisplayName("发送超长内容消息")
        void testSendVeryLongMessage() {
            // Given
            String longContent = "A".repeat(10000);
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_2))
                .thenReturn(testConversation);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message saved = invocation.getArgument(0);
                saved.setId(TEST_MESSAGE_ID);
                return saved;
            });
            doNothing().when(conversationService).updateLastMessage(TEST_CONVERSATION_ID, TEST_MESSAGE_ID);

            // When
            Message result = messageService.sendMessage(TEST_USER_ID_1, TEST_USER_ID_2, longContent);

            // Then
            assertNotNull(result);
            assertEquals(longContent, result.getContent());
        }
    }

    // ==================== 消息查询测试 ====================

    @Nested
    @DisplayName("消息查询测试")
    class MessageQueryTests {

        @Test
        @DisplayName("分页获取对话消息（按时间倒序）")
        void testGetMessagesByConversation() {
            // Given
            List<Message> messages = Arrays.asList(testMessage);
            Pageable pageable = PageRequest.of(0, 20);
            
            when(conversationRepository.findById(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(testConversation));
            when(messageRepository.findByConversationIdOrderByCreatedAtDesc(TEST_CONVERSATION_ID, pageable))
                .thenReturn(messages);
            // When
            List<Message> result = messageService.getMessagesByConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1, 0, 20);

            // Then
            assertEquals(1, result.size());
            assertEquals(TEST_MESSAGE_ID, result.get(0).getId());
        }

        @Test
        @DisplayName("根据ID获取单条消息")
        void testGetMessageById() {
            // Given
            when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(testMessage));

            // When
            Message result = messageService.getMessageById(TEST_MESSAGE_ID);

            // Then
            assertNotNull(result);
            assertEquals(TEST_MESSAGE_ID, result.getId());
            assertEquals(TEST_CONTENT, result.getContent());
        }

        @Test
        @DisplayName("获取不存在的消息应抛出异常")
        void testGetMessageByIdNotFound() {
            // Given
            when(messageRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageService.getMessageById(999L));
            assertEquals("消息不存在", exception.getMessage());
        }
    }

    // ==================== 已读状态测试 ====================

    @Nested
    @DisplayName("已读状态测试")
    class ReadStatusTests {

        @Test
        @DisplayName("标记单条消息为已读")
        void testMarkMessageAsRead() {
            // Given
            when(messageRepository.markMessageAsRead(TEST_MESSAGE_ID, TEST_USER_ID_2)).thenReturn(1);

            // When
            int result = messageService.markMessageAsRead(TEST_MESSAGE_ID, TEST_USER_ID_2);

            // Then
            assertEquals(1, result);
            verify(messageRepository).markMessageAsRead(TEST_MESSAGE_ID, TEST_USER_ID_2);
        }

        @Test
        @DisplayName("标记不存在的消息为已读返回0")
        void testMarkNonExistentMessageAsRead() {
            // Given
            when(messageRepository.markMessageAsRead(999L, TEST_USER_ID_2)).thenReturn(0);

            // When
            int result = messageService.markMessageAsRead(999L, TEST_USER_ID_2);

            // Then
            assertEquals(0, result);
        }

        @Test
        @DisplayName("标记与特定用户的所有消息为已读")
        void testMarkAllMessagesAsReadWithUser() {
            // Given
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(messageRepository.markAllMessagesAsReadBetweenUsers(TEST_USER_ID_2, TEST_USER_ID_1))
                .thenReturn(5);

            // When
            int result = messageService.markAllMessagesAsReadWithUser(TEST_USER_ID_2, TEST_USER_ID_1);

            // Then
            assertEquals(5, result);
        }

        
    }

    // ==================== 未读消息统计测试 ====================

    @Nested
    @DisplayName("未读消息统计测试")
    class UnreadCountTests {

        @Test
        @DisplayName("获取用户未读消息总数")
        void testGetTotalUnreadCount() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(messageRepository.countUnreadMessagesByUserId(TEST_USER_ID_1)).thenReturn(8L);

            // When
            long result = messageService.getTotalUnreadCount(TEST_USER_ID_1);

            // Then
            assertEquals(8L, result);
        }

        @Test
        @DisplayName("用户没有未读消息时返回0")
        void testGetTotalUnreadCountZero() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(messageRepository.countUnreadMessagesByUserId(TEST_USER_ID_1)).thenReturn(0L);

            // When
            long result = messageService.getTotalUnreadCount(TEST_USER_ID_1);

            // Then
            assertEquals(0L, result);
        }

        @Test
        @DisplayName("获取用户与特定用户的未读消息数")
        void testGetUnreadCountWithUser() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.countUnreadMessagesBetweenUsers(TEST_USER_ID_1, TEST_USER_ID_2))
                .thenReturn(3L);

            // When
            long result = messageService.getUnreadCountWithUser(TEST_USER_ID_1, TEST_USER_ID_2);

            // Then
            assertEquals(3L, result);
        }
    }

    // ==================== 消息列表查询测试 ====================

    @Nested
    @DisplayName("消息列表查询测试")
    class MessageListTests {

        @Test
        @DisplayName("获取用户接收的最新消息")
        void testGetLatestReceivedMessages() {
            // Given
            List<Message> messages = Arrays.asList(testMessage);
            Pageable pageable = PageRequest.of(0, 10);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.findByReceiverIdOrderByCreatedAtDesc(TEST_USER_ID_2, pageable))
                .thenReturn(messages);

            // When
            List<Message> result = messageService.getLatestReceivedMessages(TEST_USER_ID_2, 0, 10);

            // Then
            assertEquals(1, result.size());
            assertEquals(TEST_USER_ID_1, result.get(0).getSenderId());
        }

        @Test
        @DisplayName("获取用户发送的最新消息")
        void testGetLatestSentMessages() {
            // Given
            List<Message> messages = Arrays.asList(testMessage);
            Pageable pageable = PageRequest.of(0, 10);
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(messageRepository.findBySenderIdOrderByCreatedAtDesc(TEST_USER_ID_1, pageable))
                .thenReturn(messages);

            // When
            List<Message> result = messageService.getLatestSentMessages(TEST_USER_ID_1, 0, 10);

            // Then
            assertEquals(1, result.size());
            assertEquals(TEST_USER_ID_1, result.get(0).getSenderId());
        }

        @Test
        @DisplayName("获取用户所有相关消息")
        void testGetAllUserMessages() {
            // Given
            List<Message> messages = Arrays.asList(testMessage);
            Pageable pageable = PageRequest.of(0, 20);
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(messageRepository.findAllByUserIdOrderByCreatedAtDesc(TEST_USER_ID_1, pageable))
                .thenReturn(messages);

            // When
            List<Message> result = messageService.getAllUserMessages(TEST_USER_ID_1, 0, 20);

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("获取按对话分组的最新消息列表")
        void testGetLatestMessagesGroupedByConversation() {
            // Given
            List<Message> latestMessages = Arrays.asList(testMessage);
            Pageable pageable = PageRequest.of(0, 10);
            
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(messageRepository.findLatestMessagesByUser(TEST_USER_ID_1, pageable))
                .thenReturn(latestMessages);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.countUnreadMessagesInConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1))
                .thenReturn(2L);

            // When
            List<MessageService.LatestConversationMessageDto> result = 
                messageService.getLatestMessagesGroupedByConversation(TEST_USER_ID_1, 0, 10);

            // Then
            assertEquals(1, result.size());
            MessageService.LatestConversationMessageDto dto = result.get(0);
            assertEquals(TEST_CONVERSATION_ID, dto.getConversationId());
            assertEquals(TEST_USER_ID_2, dto.getOtherUser().getId());
            assertEquals(2L, dto.getUnreadCount());
            assertEquals(TEST_MESSAGE_ID, dto.getMessage().getId());
        }
    }

    // ==================== 消息删除测试 ====================

    @Nested
    @DisplayName("消息删除测试")
    class DeleteMessageTests {

        @Test
        @DisplayName("发送者成功删除自己的消息")
        void testDeleteMessageBySender() {
            // Given
            when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(testMessage));
            doNothing().when(messageRepository).delete(testMessage);

            // When
            messageService.deleteMessage(TEST_MESSAGE_ID, TEST_USER_ID_1);

            // Then
            verify(messageRepository).delete(testMessage);
        }

        @Test
        @DisplayName("接收者不能删除发送者的消息")
        void testReceiverCannotDeleteMessage() {
            // Given
            when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(testMessage));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageService.deleteMessage(TEST_MESSAGE_ID, TEST_USER_ID_2));
            assertEquals("无权删除此消息", exception.getMessage());

            verify(messageRepository, never()).delete(any());
        }

        @Test
        @DisplayName("删除不存在的消息应抛出异常")
        void testDeleteNonExistentMessage() {
            // Given
            when(messageRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageService.deleteMessage(999L, TEST_USER_ID_1));
            assertEquals("消息不存在", exception.getMessage());
        }
    }

    // ==================== 边界和异常测试 ====================

    @Nested
    @DisplayName("边界和异常测试")
    class BoundaryAndExceptionTests {

        @Test
        @DisplayName("分页参数边界测试")
        void testPaginationBoundary() {
            // Given
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(messageRepository.findByReceiverIdOrderByCreatedAtDesc(eq(TEST_USER_ID_1), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

            // When
            List<Message> result = messageService.getLatestReceivedMessages(TEST_USER_ID_1, 0, Integer.MAX_VALUE);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("负数页码处理")
        void testNegativePageNumber() {
            // Given
            try{
                Pageable pageable = PageRequest.of(-1, 10);
                when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
                when(messageRepository.findByReceiverIdOrderByCreatedAtDesc(eq(TEST_USER_ID_1), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

                // When
                List<Message> result = messageService.getLatestReceivedMessages(TEST_USER_ID_1, -1, 10);
                assertEquals(null, result);
            }catch (IllegalArgumentException e) {
                // Then
                assertEquals("Page index must not be less than zero", e.getMessage());
            }            
        }

        @Test
        @DisplayName("获取消息时对话权限验证失败")
        void testGetMessagesWithInvalidConversationPermission() {
            // Given
            when(conversationService.getConversationDetail(TEST_CONVERSATION_ID, TEST_USER_ID_1))
                .thenThrow(new RuntimeException("无权访问此对话"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageService.getAllMessagesInConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1));
            assertEquals("无权访问此对话", exception.getMessage());
        }

        @Test
        @DisplayName("标记对话消息已读时对话不存在")
        void testMarkReadInNonExistentConversation() {
            // Given
            when(conversationService.getConversationDetail(999L, TEST_USER_ID_1))
                .thenThrow(new RuntimeException("对话不存在"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageService.markAllMessagesAsReadInConversation(999L, TEST_USER_ID_1));
            assertEquals("对话不存在", exception.getMessage());
        }

        
        @Test
        @DisplayName("消息时间戳验证")
        void testMessageTimestamp() {
            // Given
            LocalDateTime beforeSend = LocalDateTime.now();
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_2))
                .thenReturn(testConversation);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message saved = invocation.getArgument(0);
                saved.setId(TEST_MESSAGE_ID);
                return saved;
            });
            doNothing().when(conversationService).updateLastMessage(TEST_CONVERSATION_ID, TEST_MESSAGE_ID);

            // When
            Message result = messageService.sendMessage(TEST_USER_ID_1, TEST_USER_ID_2, TEST_CONTENT);
            LocalDateTime afterSend = LocalDateTime.now();

            // Then
            assertNotNull(result.getCreatedAt());
            assertTrue(result.getCreatedAt().isAfter(beforeSend) || result.getCreatedAt().equals(beforeSend));
            assertTrue(result.getCreatedAt().isBefore(afterSend) || result.getCreatedAt().equals(afterSend));
        }
    }
}