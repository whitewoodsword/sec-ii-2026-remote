package com.example.backend.controller;

import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.service.MessageService;
import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            User sender = userService.getUserByToken(params.get("token"));
            Message message = messageService.sendMessage(
                sender.getId(),
                Long.parseLong(params.get("receiverId")),
                params.get("content"),
                params.getOrDefault("type", Message.TYPE_CHAT)
            );
            result.put("success", true);
            result.put("message", "发送成功");
            result.put("data", message);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/conversation")
    public Map<String, Object> getConversation(@RequestParam String token,
                                               @RequestParam Long withUserId) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserByToken(token);
            List<Message> messages = messageService.getConversation(user.getId(), withUserId);
            result.put("success", true);
            result.put("data", messages);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/conversations")
    public Map<String, Object> getConversations(@RequestParam String token) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserByToken(token);
            List<Map<String, Object>> conversations = messageService.getConversationList(user.getId());
            result.put("success", true);
            result.put("data", conversations);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/notifications")
    public Map<String, Object> getNotifications(@RequestParam String token) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserByToken(token);
            List<Message> notifications = messageService.getSystemNotifications(user.getId());
            result.put("success", true);
            result.put("data", notifications);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/read/{messageId}")
    public Map<String, Object> markAsRead(@PathVariable Long messageId) {
        Map<String, Object> result = new HashMap<>();
        try {
            messageService.markAsRead(messageId);
            result.put("success", true);
            result.put("message", "已标记为已读");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/read-all")
    public Map<String, Object> markAllAsRead(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserByToken(params.get("token"));
            messageService.markAllAsRead(user.getId());
            result.put("success", true);
            result.put("message", "全部标记为已读");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/read-conversation")
    public Map<String, Object> markConversationAsRead(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserByToken(params.get("token"));
            long senderId = Long.parseLong(params.get("senderId"));
            messageService.markConversationAsRead(senderId, user.getId());
            result.put("success", true);
            result.put("message", "对话标记为已读");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/unread-count")
    public Map<String, Object> getUnreadCount(@RequestParam String token) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserByToken(token);
            long count = messageService.getUnreadCount(user.getId());
            result.put("success", true);
            result.put("count", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
