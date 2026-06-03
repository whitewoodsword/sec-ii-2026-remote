package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.entity.Conversation;
import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.MessageRepository;

@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserService userService;

    /**
     * 获取或创建两个用户之间的对话
     */
    @Transactional
    public Conversation getOrCreateConversation(Long user1Id, Long user2Id) {
        // 验证用户存在
        userService.getUserById(user1Id);
        userService.getUserById(user2Id);
        
        if (user1Id.equals(user2Id)) {
            throw new RuntimeException("不能与自己创建对话");
        }
        
        Optional<Conversation> existingConv = conversationRepository.findConversationBetweenUsers(user1Id, user2Id);
        
        if (existingConv.isPresent()) {
            return existingConv.get();
        }
        
        Conversation conversation = new Conversation(user1Id, user2Id);
        return conversationRepository.save(conversation);
    }
    
    /**
     * 获取用户的对话列表（分页）
     */
    public List<ConversationDto> getUserConversations(Long userId, int page, int size) {
        userService.getUserById(userId);
        Pageable pageable = PageRequest.of(page, size);
        List<Conversation> conversations = conversationRepository.findAllByUserIdOrderByLastMessageTimeDesc(userId, pageable);
        
        return conversations.stream()
                .map(conv -> convertToDto(conv, userId))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取对话详情（包含消息历史）
     */
    public ConversationDetailDto getConversationDetail(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("对话不存在"));
        
        // 验证用户是否属于该对话
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new RuntimeException("无权访问此对话");
        }
        
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        
        // 获取对方用户信息
        Long otherUserId = conversation.getUser1Id().equals(userId) ? conversation.getUser2Id() : conversation.getUser1Id();
        User otherUser = userService.getUserById(otherUserId);
        
        ConversationDetailDto dto = new ConversationDetailDto();
        dto.setConversationId(conversationId);
        dto.setOtherUser(otherUser);
        dto.setMessages(messages);
        dto.setUnreadCount(messageRepository.countUnreadMessagesInConversation(conversationId, userId));
        
        return dto;
    }
    
    /**
     * 获取两个用户之间的对话（按时间顺序的完整消息）
     */
    public List<Message> getMessagesBetweenUsers(Long userId, Long otherUserId) {
        userService.getUserById(userId);
        userService.getUserById(otherUserId);
        
        return messageRepository.findMessagesBetweenUsers(userId, otherUserId);
    }
    
    /**
     * 更新对话的最后消息
     */
    @Transactional
    public void updateLastMessage(Long conversationId, Long messageId) {
        conversationRepository.updateLastMessage(conversationId, messageId, LocalDateTime.now());
    }
    
    /**
     * 删除对话（同时删除所有相关消息）
     */
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("对话不存在"));
        
        // 验证用户权限
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new RuntimeException("无权删除此对话");
        }
        
        // 删除所有消息
        messageRepository.deleteAllByConversationId(conversationId);
        // 删除对话
        conversationRepository.delete(conversation);
    }
    
    /**
     * 将Conversation转换为DTO
     */
    private ConversationDto convertToDto(Conversation conversation, Long currentUserId) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setLastMessageTime(conversation.getLastMessageTime());
        
        // 获取对方用户信息
        Long otherUserId = conversation.getUser1Id().equals(currentUserId) ? conversation.getUser2Id() : conversation.getUser1Id();
        User otherUser = userService.getUserById(otherUserId);
        dto.setOtherUser(otherUser);
        
        // 获取最后一条消息
        if (conversation.getLastMessageId() != null) {
            Optional<Message> lastMessageOpt = messageRepository.findById(conversation.getLastMessageId());
            lastMessageOpt.ifPresent(dto::setLastMessage);
        }
        
        // 统计未读消息数
        long unreadCount = messageRepository.countUnreadMessagesInConversation(conversation.getId(), currentUserId);
        dto.setUnreadCount(unreadCount);
        
        return dto;
    }
    
    // ========== DTO类 ==========
    
    public static class ConversationDto {
        private Long id;
        private User otherUser;
        private Message lastMessage;
        private Long unreadCount;
        private LocalDateTime createdAt;
        private LocalDateTime lastMessageTime;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public User getOtherUser() { return otherUser; }
        public void setOtherUser(User otherUser) { this.otherUser = otherUser; }
        public Message getLastMessage() { return lastMessage; }
        public void setLastMessage(Message lastMessage) { this.lastMessage = lastMessage; }
        public Long getUnreadCount() { return unreadCount; }
        public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getLastMessageTime() { return lastMessageTime; }
        public void setLastMessageTime(LocalDateTime lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    }
    
    public static class ConversationDetailDto {
        private Long conversationId;
        private User otherUser;
        private List<Message> messages;
        private Long unreadCount;
        
        // Getters and Setters
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public User getOtherUser() { return otherUser; }
        public void setOtherUser(User otherUser) { this.otherUser = otherUser; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
        public Long getUnreadCount() { return unreadCount; }
        public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }
    }
}