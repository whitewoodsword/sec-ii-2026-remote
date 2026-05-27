package com.example.backend.service;

import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Message sendMessage(Long senderId, Long receiverId, String content, String type) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("消息内容不能为空");
        }
        if (!userRepository.existsById(senderId)) {
            throw new RuntimeException("发送者不存在");
        }
        if (!userRepository.existsById(receiverId)) {
            throw new RuntimeException("接收者不存在");
        }
        Message message = new Message(senderId, receiverId, content.trim(), type);
        return messageRepository.save(message);
    }

    public List<Message> getConversation(Long user1, Long user2) {
        return messageRepository.findConversation(user1, user2);
    }

    public List<Map<String, Object>> getConversationList(Long userId) {
        Set<Long> partnerIds = new LinkedHashSet<>();
        partnerIds.addAll(messageRepository.findChatSenderIds(userId));
        partnerIds.addAll(messageRepository.findChatReceiverIds(userId));

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Long partnerId : partnerIds) {
            Optional<User> partnerOpt = userRepository.findById(partnerId);
            if (partnerOpt.isEmpty()) continue;
            User partner = partnerOpt.get();

            Map<String, Object> conv = new HashMap<>();
            conv.put("userId", partner.getId());
            conv.put("userName", partner.getName());
            conv.put("avatarPath", partner.getAvatarPath());

            Optional<Message> lastMsg = messageRepository.findLastMessageBetween(userId, partnerId);
            if (lastMsg.isPresent()) {
                conv.put("lastMessage", lastMsg.get().getContent());
                conv.put("lastTime", lastMsg.get().getCreatedAt().toString());
            } else {
                conv.put("lastMessage", "");
                conv.put("lastTime", "");
            }

            long unread = messageRepository.countBySenderIdAndReceiverIdAndIsReadFalse(partnerId, userId);
            conv.put("unreadCount", unread);

            conversations.add(conv);
        }

        conversations.sort((a, b) -> {
            String timeA = (String) a.get("lastTime");
            String timeB = (String) b.get("lastTime");
            if (timeA.isEmpty()) return 1;
            if (timeB.isEmpty()) return -1;
            return timeB.compareTo(timeA);
        });

        return conversations;
    }

    public List<Message> getSystemNotifications(Long userId) {
        return messageRepository.findByReceiverIdAndTypeOrderByCreatedAtDesc(userId, Message.TYPE_SYSTEM);
    }

    @Transactional
    public void markAsRead(Long messageId) {
        messageRepository.markAsRead(messageId);
    }

    @Transactional
    public void markAllAsRead(Long receiverId) {
        messageRepository.markAllAsRead(receiverId);
    }

    public long getUnreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markConversationAsRead(Long senderId, Long receiverId) {
        messageRepository.markConversationAsRead(senderId, receiverId);
    }
}
