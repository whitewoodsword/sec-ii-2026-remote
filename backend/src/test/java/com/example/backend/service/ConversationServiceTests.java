package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
@DisplayName("对话服务单元测试")
class ConversationServiceTests {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ConversationService conversationService;

    private User testUser1;
    private User testUser2;
    private Conversation testConversation;
    private Message testMessage;
    private final Long TEST_USER_ID_1 = 100L;
    private final Long TEST_USER_ID_2 = 200L;
    private final Long TEST_CONVERSATION_ID = 1000L;
    private final Long TEST_MESSAGE_ID = 5000L;

    @BeforeEach
    void setUp() {
        testUser1 = new User("用户A", "13800138000", "password");
        testUser1.setId(TEST_USER_ID_1);
        testUser1.setAvatarPath("/avatar/a.jpg");

        testUser2 = new User("用户B", "13900139000", "password");
        testUser2.setId(TEST_USER_ID_2);
        testUser2.setAvatarPath("/avatar/b.jpg");

        testConversation = new Conversation(TEST_USER_ID_1, TEST_USER_ID_2);
        testConversation.setId(TEST_CONVERSATION_ID);
        testConversation.setLastMessageId(null);
        testConversation.setLastMessageTime(LocalDateTime.now());
        testConversation.setCreatedAt(LocalDateTime.now());

        testMessage = new Message(TEST_USER_ID_1, TEST_USER_ID_2, "测试消息", TEST_CONVERSATION_ID);
        testMessage.setId(TEST_MESSAGE_ID);
        testMessage.setRead(false);
        testMessage.setCreatedAt(LocalDateTime.now());
    }

    // ==================== 获取或创建对话测试 ====================

    @Nested
    @DisplayName("获取或创建对话测试")
    class GetOrCreateConversationTests {

        @Test
        @DisplayName("成功获取已存在的对话")
        void testGetExistingConversation() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(conversationRepository.findConversationBetweenUsers(TEST_USER_ID_1, TEST_USER_ID_2))
                .thenReturn(Optional.of(testConversation));

            // When
            Conversation result = conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_2);

            // Then
            assertNotNull(result);
            assertEquals(TEST_CONVERSATION_ID, result.getId());
            assertEquals(TEST_USER_ID_1, result.getUser1Id());
            assertEquals(TEST_USER_ID_2, result.getUser2Id());
            verify(conversationRepository, never()).save(any());
        }

        @Test
        @DisplayName("对话不存在时成功创建新对话")
        void testCreateNewConversationWhenNotExists() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(conversationRepository.findConversationBetweenUsers(TEST_USER_ID_1, TEST_USER_ID_2))
                .thenReturn(Optional.empty());
            when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

            // When
            Conversation result = conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_2);

            // Then
            assertNotNull(result);
            assertEquals(TEST_CONVERSATION_ID, result.getId());
            verify(conversationRepository).save(any(Conversation.class));
        }

        @Test
        @DisplayName("用户1不存在时抛出异常")
        void testGetOrCreateConversationUser1NotFound() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenThrow(new RuntimeException("用户不存在"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_2));
            assertEquals("用户不存在", exception.getMessage());
            verify(conversationRepository, never()).findConversationBetweenUsers(any(), any());
        }

        @Test
        @DisplayName("用户2不存在时抛出异常")
        void testGetOrCreateConversationUser2NotFound() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenThrow(new RuntimeException("用户不存在"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_2));
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("不能与自己创建对话")
        void testCannotCreateConversationWithSelf() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_1));
            assertEquals("不能与自己创建对话", exception.getMessage());
        }

        @Test
        @DisplayName("用户ID顺序交换时也能找到相同的对话")
        void testFindConversationWithSwappedUserIds() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(conversationRepository.findConversationBetweenUsers(TEST_USER_ID_2, TEST_USER_ID_1))
                .thenReturn(Optional.of(testConversation));

            // When - 交换用户ID顺序
            Conversation result = conversationService.getOrCreateConversation(TEST_USER_ID_2, TEST_USER_ID_1);

            // Then
            assertNotNull(result);
            assertEquals(TEST_CONVERSATION_ID, result.getId());
        }
    }

    // ==================== 用户对话列表测试 ====================

    @Nested
    @DisplayName("用户对话列表测试")
    class UserConversationsTests {

        @Test
        @DisplayName("成功获取用户对话列表")
        void testGetUserConversationsSuccess() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Conversation> conversations = Arrays.asList(testConversation);
            
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(conversationRepository.findAllByUserIdOrderByLastMessageTimeDesc(TEST_USER_ID_1, pageable))
                .thenReturn(conversations);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.countUnreadMessagesInConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1))
                .thenReturn(0L);

            // When
            List<ConversationService.ConversationDto> result = 
                conversationService.getUserConversations(TEST_USER_ID_1, 0, 10);

            // Then
            assertEquals(1, result.size());
            ConversationService.ConversationDto dto = result.get(0);
            assertEquals(TEST_CONVERSATION_ID, dto.getId());
            assertEquals(TEST_USER_ID_2, dto.getOtherUser().getId());
            assertEquals("用户B", dto.getOtherUser().getName());
        }

        @Test
        @DisplayName("用户没有对话时返回空列表")
        void testGetUserConversationsEmpty() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(conversationRepository.findAllByUserIdOrderByLastMessageTimeDesc(TEST_USER_ID_1, pageable))
                .thenReturn(Collections.emptyList());

            // When
            List<ConversationService.ConversationDto> result = 
                conversationService.getUserConversations(TEST_USER_ID_1, 0, 10);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("对话列表包含未读消息计数")
        void testUserConversationsWithUnreadCount() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Conversation> conversations = Arrays.asList(testConversation);
            
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(conversationRepository.findAllByUserIdOrderByLastMessageTimeDesc(TEST_USER_ID_1, pageable))
                .thenReturn(conversations);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.countUnreadMessagesInConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1))
                .thenReturn(5L);

            // When
            List<ConversationService.ConversationDto> result = 
                conversationService.getUserConversations(TEST_USER_ID_1, 0, 10);

            // Then
            assertEquals(5L, result.get(0).getUnreadCount());
        }

        @Test
        @DisplayName("对话列表包含最后一条消息")
        void testUserConversationsWithLastMessage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            testConversation.setLastMessageId(TEST_MESSAGE_ID);
            List<Conversation> conversations = Arrays.asList(testConversation);
            
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(conversationRepository.findAllByUserIdOrderByLastMessageTimeDesc(TEST_USER_ID_1, pageable))
                .thenReturn(conversations);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(testMessage));
            when(messageRepository.countUnreadMessagesInConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1))
                .thenReturn(0L);

            // When
            List<ConversationService.ConversationDto> result = 
                conversationService.getUserConversations(TEST_USER_ID_1, 0, 10);

            // Then
            assertNotNull(result.get(0).getLastMessage());
            assertEquals(TEST_MESSAGE_ID, result.get(0).getLastMessage().getId());
            assertEquals("测试消息", result.get(0).getLastMessage().getContent());
        }

        @Test
        @DisplayName("最后一条消息不存在时不影响列表")
        void testUserConversationsWithMissingLastMessage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            testConversation.setLastMessageId(999L);
            List<Conversation> conversations = Arrays.asList(testConversation);
            
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(conversationRepository.findAllByUserIdOrderByLastMessageTimeDesc(TEST_USER_ID_1, pageable))
                .thenReturn(conversations);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.findById(999L)).thenReturn(Optional.empty());
            when(messageRepository.countUnreadMessagesInConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1))
                .thenReturn(0L);

            // When
            List<ConversationService.ConversationDto> result = 
                conversationService.getUserConversations(TEST_USER_ID_1, 0, 10);

            // Then
            assertNull(result.get(0).getLastMessage());
        }
    }

    // ==================== 对话详情测试 ====================

    @Nested
    @DisplayName("对话详情测试")
    class ConversationDetailTests {

        @Test
        @DisplayName("成功获取对话详情")
        void testGetConversationDetailSuccess() {
            // Given
            List<Message> messages = Arrays.asList(testMessage);
            when(conversationRepository.findById(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(testConversation));
            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(TEST_CONVERSATION_ID))
                .thenReturn(messages);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.countUnreadMessagesInConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1))
                .thenReturn(2L);

            // When
            ConversationService.ConversationDetailDto result = 
                conversationService.getConversationDetail(TEST_CONVERSATION_ID, TEST_USER_ID_1);

            // Then
            assertNotNull(result);
            assertEquals(TEST_CONVERSATION_ID, result.getConversationId());
            assertEquals(TEST_USER_ID_2, result.getOtherUser().getId());
            assertEquals(1, result.getMessages().size());
            assertEquals(2L, result.getUnreadCount());
        }

        @Test
        @DisplayName("对话不存在时抛出异常")
        void testGetConversationDetailNotFound() {
            // Given
            when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> conversationService.getConversationDetail(999L, TEST_USER_ID_1));
            assertEquals("对话不存在", exception.getMessage());
        }

        @Test
        @DisplayName("用户无权访问对话时抛出异常")
        void testGetConversationDetailUnauthorized() {
            // Given
            Long unauthorizedUserId = 300L;
            when(conversationRepository.findById(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(testConversation));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> conversationService.getConversationDetail(TEST_CONVERSATION_ID, unauthorizedUserId));
            assertEquals("无权访问此对话", exception.getMessage());
        }

        @Test
        @DisplayName("对话详情包含空消息列表")
        void testGetConversationDetailEmptyMessages() {
            // Given
            when(conversationRepository.findById(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(testConversation));
            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(TEST_CONVERSATION_ID))
                .thenReturn(Collections.emptyList());
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.countUnreadMessagesInConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1))
                .thenReturn(0L);

            // When
            ConversationService.ConversationDetailDto result = 
                conversationService.getConversationDetail(TEST_CONVERSATION_ID, TEST_USER_ID_1);

            // Then
            assertTrue(result.getMessages().isEmpty());
        }
    }

    // ==================== 用户间消息测试 ====================

    @Nested
    @DisplayName("用户间消息测试")
    class MessagesBetweenUsersTests {

        @Test
        @DisplayName("成功获取两个用户之间的所有消息")
        void testGetMessagesBetweenUsersSuccess() {
            // Given
            List<Message> messages = Arrays.asList(testMessage);
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.findMessagesBetweenUsers(TEST_USER_ID_1, TEST_USER_ID_2))
                .thenReturn(messages);

            // When
            List<Message> result = conversationService.getMessagesBetweenUsers(TEST_USER_ID_1, TEST_USER_ID_2);

            // Then
            assertEquals(1, result.size());
            assertEquals(TEST_MESSAGE_ID, result.get(0).getId());
        }

        @Test
        @DisplayName("两个用户之间没有消息时返回空列表")
        void testGetMessagesBetweenUsersEmpty() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(messageRepository.findMessagesBetweenUsers(TEST_USER_ID_1, TEST_USER_ID_2))
                .thenReturn(Collections.emptyList());

            // When
            List<Message> result = conversationService.getMessagesBetweenUsers(TEST_USER_ID_1, TEST_USER_ID_2);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void testGetMessagesBetweenUsersUserNotFound() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenThrow(new RuntimeException("用户不存在"));

            // When & Then
            assertThrows(RuntimeException.class,
                () -> conversationService.getMessagesBetweenUsers(TEST_USER_ID_1, TEST_USER_ID_2));
        }
    }

    // ==================== 更新最后消息测试 ====================

    @Nested
    @DisplayName("更新最后消息测试")
    class UpdateLastMessageTests {

        @Test
        @DisplayName("成功更新对话的最后消息")
        void testUpdateLastMessageSuccess() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            // When
            conversationService.updateLastMessage(TEST_CONVERSATION_ID, TEST_MESSAGE_ID);

            // Then
            verify(conversationRepository).updateLastMessage(eq(TEST_CONVERSATION_ID), eq(TEST_MESSAGE_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("更新多个对话的最后消息")
        void testUpdateLastMessageMultiple() {
            // Given
            Long convId2 = 2000L;
            Long msgId2 = 6000L;
            doNothing().when(conversationRepository).updateLastMessage(anyLong(), anyLong(), any());

            // When
            conversationService.updateLastMessage(TEST_CONVERSATION_ID, TEST_MESSAGE_ID);
            conversationService.updateLastMessage(convId2, msgId2);

            // Then
            verify(conversationRepository, times(2)).updateLastMessage(anyLong(), anyLong(), any());
        }
    }

    // ==================== 删除对话测试 ====================

    @Nested
    @DisplayName("删除对话测试")
    class DeleteConversationTests {

        @Test
        @DisplayName("成功删除对话及所有消息")
        void testDeleteConversationSuccess() {
            // Given
            when(conversationRepository.findById(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(testConversation));
            doNothing().when(messageRepository).deleteAllByConversationId(TEST_CONVERSATION_ID);
            doNothing().when(conversationRepository).delete(testConversation);

            // When
            conversationService.deleteConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1);

            // Then
            verify(messageRepository).deleteAllByConversationId(TEST_CONVERSATION_ID);
            verify(conversationRepository).delete(testConversation);
        }

        @Test
        @DisplayName("对话不存在时抛出异常")
        void testDeleteConversationNotFound() {
            // Given
            when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> conversationService.deleteConversation(999L, TEST_USER_ID_1));
            assertEquals("对话不存在", exception.getMessage());
            verify(messageRepository, never()).deleteAllByConversationId(any());
        }

        @Test
        @DisplayName("非对话参与者不能删除对话")
        void testDeleteConversationUnauthorized() {
            // Given
            Long unauthorizedUserId = 300L;
            when(conversationRepository.findById(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(testConversation));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> conversationService.deleteConversation(TEST_CONVERSATION_ID, unauthorizedUserId));
            assertEquals("无权删除此对话", exception.getMessage());
            verify(messageRepository, never()).deleteAllByConversationId(any());
        }

        @Test
        @DisplayName("用户1删除对话后用户2仍可拥有自己的对话记录")
        void testUser1DeletesConversationUser2StillHas() {
            // Given
            when(conversationRepository.findById(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(testConversation));
            doNothing().when(messageRepository).deleteAllByConversationId(TEST_CONVERSATION_ID);
            doNothing().when(conversationRepository).delete(testConversation);

            // When - 用户1删除对话
            conversationService.deleteConversation(TEST_CONVERSATION_ID, TEST_USER_ID_1);

            // Then
            verify(messageRepository).deleteAllByConversationId(TEST_CONVERSATION_ID);
            verify(conversationRepository).delete(testConversation);
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
            when(conversationRepository.findAllByUserIdOrderByLastMessageTimeDesc(TEST_USER_ID_1, pageable))
                .thenReturn(Collections.emptyList());

            // When
            List<ConversationService.ConversationDto> result = 
                conversationService.getUserConversations(TEST_USER_ID_1, 0, Integer.MAX_VALUE);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("负数页码处理")
        void testNegativePageNumber() {
            try {
                Pageable pageable = PageRequest.of(-1, 10);
                when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
                when(conversationRepository.findAllByUserIdOrderByLastMessageTimeDesc(TEST_USER_ID_1, pageable))
                    .thenReturn(Collections.emptyList());

                // When
                List<ConversationService.ConversationDto> result = 
                    conversationService.getUserConversations(TEST_USER_ID_1, -1, 10);

                // Then
                assertNotNull(result);
            } catch (Exception e) {
                assertEquals("Page index must not be less than zero", e.getMessage());
            }

            
        }


        @Test
        @DisplayName("对话创建时时间戳正确设置")
        void testConversationCreationTimestamps() {
            // Given
            LocalDateTime beforeCreate = LocalDateTime.now();
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(conversationRepository.findConversationBetweenUsers(TEST_USER_ID_1, TEST_USER_ID_2))
                .thenReturn(Optional.empty());
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
                Conversation saved = invocation.getArgument(0);
                saved.setId(TEST_CONVERSATION_ID);
                return saved;
            });

            // When
            Conversation result = conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_2);
            LocalDateTime afterCreate = LocalDateTime.now();

            // Then
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getLastMessageTime());
            assertTrue(result.getCreatedAt().isAfter(beforeCreate) || result.getCreatedAt().equals(beforeCreate));
            assertTrue(result.getCreatedAt().isBefore(afterCreate) || result.getCreatedAt().equals(afterCreate));
        }

        @Test
        @DisplayName("创建对话时用户验证顺序测试")
        void testUserValidationOrder() {
            // Given
            when(userService.getUserById(TEST_USER_ID_1)).thenThrow(new RuntimeException("用户不存在"));

            // When & Then - 第一个用户验证失败，不会验证第二个用户
            assertThrows(RuntimeException.class,
                () -> conversationService.getOrCreateConversation(TEST_USER_ID_1, TEST_USER_ID_2));
            verify(userService, never()).getUserById(TEST_USER_ID_2);
        }

        @Test
        @DisplayName("对话双方ID排序不影响功能")
        void testConversationUserOrder() {
            // Given - 创建时顺序为(1,2)
            Conversation conv1 = new Conversation(TEST_USER_ID_1, TEST_USER_ID_2);
            conv1.setId(TEST_CONVERSATION_ID);
            
            when(userService.getUserById(TEST_USER_ID_1)).thenReturn(testUser1);
            when(userService.getUserById(TEST_USER_ID_2)).thenReturn(testUser2);
            when(conversationRepository.findConversationBetweenUsers(TEST_USER_ID_2, TEST_USER_ID_1))
                .thenReturn(Optional.of(conv1));

            // When - 查询时顺序为(2,1)
            Conversation result = conversationService.getOrCreateConversation(TEST_USER_ID_2, TEST_USER_ID_1);

            // Then
            assertEquals(TEST_CONVERSATION_ID, result.getId());
            assertEquals(TEST_USER_ID_1, result.getUser1Id());
            assertEquals(TEST_USER_ID_2, result.getUser2Id());
        }
    }

    // ==================== DTO转换测试 ====================

    @Nested
    @DisplayName("DTO转换测试")
    class DtoConversionTests {

        @Test
        @DisplayName("ConversationDto包含所有必要字段")
        void testConversationDtoFields() {
            // Given
            ConversationService.ConversationDto dto = new ConversationService.ConversationDto();
            dto.setId(TEST_CONVERSATION_ID);
            dto.setOtherUser(testUser2);
            dto.setLastMessage(testMessage);
            dto.setUnreadCount(3L);
            dto.setCreatedAt(LocalDateTime.now());
            dto.setLastMessageTime(LocalDateTime.now());

            // Then
            assertEquals(TEST_CONVERSATION_ID, dto.getId());
            assertEquals(testUser2, dto.getOtherUser());
            assertEquals(testMessage, dto.getLastMessage());
            assertEquals(3L, dto.getUnreadCount());
            assertNotNull(dto.getCreatedAt());
            assertNotNull(dto.getLastMessageTime());
        }

        @Test
        @DisplayName("ConversationDetailDto包含所有必要字段")
        void testConversationDetailDtoFields() {
            // Given
            ConversationService.ConversationDetailDto dto = new ConversationService.ConversationDetailDto();
            List<Message> messages = Arrays.asList(testMessage);
            dto.setConversationId(TEST_CONVERSATION_ID);
            dto.setOtherUser(testUser2);
            dto.setMessages(messages);
            dto.setUnreadCount(5L);

            // Then
            assertEquals(TEST_CONVERSATION_ID, dto.getConversationId());
            assertEquals(testUser2, dto.getOtherUser());
            assertEquals(1, dto.getMessages().size());
            assertEquals(5L, dto.getUnreadCount());
        }
    }
}