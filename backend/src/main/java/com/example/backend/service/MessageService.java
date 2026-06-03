package com.example.backend.service;

import java.util.List;

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
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;  
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private UserService userService;

    /**
     * 发送消息
     */
    @Transactional
    public Message sendMessage(Long senderId, Long receiverId, String content) {
        // 验证用户存在
        userService.getUserById(senderId);
        userService.getUserById(receiverId);
        
        if (senderId.equals(receiverId)) {
            throw new RuntimeException("不能给自己发送消息");
        }
        
        // 获取或创建对话
        Conversation conversation = conversationService.getOrCreateConversation(senderId, receiverId);
        
        // 创建消息
        Message message = new Message(senderId, receiverId, content, conversation.getId());
        Message savedMessage = messageRepository.save(message);
        
        // 更新对话的最后消息
        conversationService.updateLastMessage(conversation.getId(), savedMessage.getId());
        
        return savedMessage;
    }
    
    /**
     * 获取对话中的消息（分页，按时间倒序）
     */
    public List<Message> getMessagesByConversation(Long conversationId, Long userId, int page, int size) {
        // 直接查询对话并验证用户权限
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("对话不存在"));
        
        // 验证用户是否属于该对话
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new RuntimeException("无权访问此对话");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
    }
    
    /**
     * 获取对话中的所有消息（完整历史，按时间正序）
     */
    public List<Message> getAllMessagesInConversation(Long conversationId, Long userId) {
        // 验证对话权限
        conversationService.getConversationDetail(conversationId, userId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
    
    /**
     * 获取单条消息
     */
    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
    }
    
    /**
     * 标记单条消息为已读
     */
    @Transactional
    public int markMessageAsRead(Long messageId, Long userId) {
        return messageRepository.markMessageAsRead(messageId, userId);
    }
    
    /**
     * 标记与特定用户的所有消息为已读
     */
    @Transactional
    public int markAllMessagesAsReadWithUser(Long userId, Long senderId) {
        userService.getUserById(userId);
        userService.getUserById(senderId);
        return messageRepository.markAllMessagesAsReadBetweenUsers(userId, senderId);
    }
    
    /**
     * 标记对话中的所有消息为已读
     */
    @Transactional
    public int markAllMessagesAsReadInConversation(Long conversationId, Long userId) {
        // 验证对话权限
        conversationService.getConversationDetail(conversationId, userId);
        return messageRepository.markAllMessagesAsReadInConversation(conversationId, userId);
    }
    
    /**
     * 获取用户未读消息总数
     */
    public long getTotalUnreadCount(Long userId) {
        userService.getUserById(userId);
        return messageRepository.countUnreadMessagesByUserId(userId);
    }
    
    /**
     * 获取用户与特定用户的未读消息数
     */
    public long getUnreadCountWithUser(Long userId, Long otherUserId) {
        userService.getUserById(userId);
        userService.getUserById(otherUserId);
        return messageRepository.countUnreadMessagesBetweenUsers(userId, otherUserId);
    }
    
    /**
     * 获取用户接收的最新消息（分页）
     */
    public List<Message> getLatestReceivedMessages(Long userId, int page, int size) {
        userService.getUserById(userId);
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * 获取用户发送的最新消息
     */
    public List<Message> getLatestSentMessages(Long userId, int page, int size) {
        userService.getUserById(userId);
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findBySenderIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * 获取用户所有相关消息（发送和接收，按时间倒序）
     */
    public List<Message> getAllUserMessages(Long userId, int page, int size) {
        userService.getUserById(userId);
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * 获取按对话分组的最新消息列表（类似微信首页）
     */
    public List<LatestConversationMessageDto> getLatestMessagesGroupedByConversation(Long userId, int page, int size) {
        userService.getUserById(userId);
        Pageable pageable = PageRequest.of(page, size);
        List<Message> latestMessages = messageRepository.findLatestMessagesByUser(userId, pageable);
        
        return latestMessages.stream()
                .map(msg -> {
                    LatestConversationMessageDto dto = new LatestConversationMessageDto();
                    dto.setMessage(msg);
                    dto.setConversationId(msg.getConversationId());
                    Long otherUserId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
                    dto.setOtherUser(userService.getUserById(otherUserId));
                    dto.setUnreadCount(messageRepository.countUnreadMessagesInConversation(msg.getConversationId(), userId));
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 删除消息
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = getMessageById(messageId);
        
        // 只有发送者可以删除自己的消息
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("无权删除此消息");
        }
        
        messageRepository.delete(message);
    }
    
    /**
     * 获取对话中的对方用户ID
     */
    private Long getOtherUserId(Long conversationId, Long currentUserId) {
        try{
            User otherUser = conversationService.getConversationDetail(conversationId, currentUserId).getOtherUser();
            if (otherUser.getId().equals(currentUserId) || currentUserId.equals(currentUserId)) {
                return otherUser.getId();
            }
        }catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        throw new RuntimeException("当前用户不属于该对话");
    }
    // ========== DTO类 ==========
    
    public static class LatestConversationMessageDto {
        private Message message;
        private Long conversationId;
        private User otherUser;
        private Long unreadCount;
        
        // Getters and Setters
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public User getOtherUser() { return otherUser; }
        public void setOtherUser(User otherUser) { this.otherUser = otherUser; }
        public Long getUnreadCount() { return unreadCount; }
        public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }
    }
}