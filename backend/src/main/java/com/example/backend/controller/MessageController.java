package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.entity.Message;
import com.example.backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 发送消息
     * POST /messages/send?senderId={senderId}&receiverId={receiverId}
     * Body: {"content": "message content"}
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @RequestParam Long senderId,
            @RequestParam Long receiverId,
            @RequestBody Map<String, String> requestBody) {
        
        try {
            String content = requestBody.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "消息内容不能为空"));
            }
            
            Message message = messageService.sendMessage(senderId, receiverId, content);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 获取对话中的消息（分页）
     * GET /messages/conversation/{conversationId}?userId={userId}&page=0&size=10
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMessagesByConversation(
            @PathVariable Long conversationId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            List<Message> messages = messageService.getMessagesByConversation(conversationId, userId, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", messages);
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 获取对话中的所有消息（完整历史）
     * GET /messages/conversation/{conversationId}/all?userId={userId}
     */
    @GetMapping("/conversation/{conversationId}/all")
    public ResponseEntity<ApiResponse<List<Message>>> getAllMessagesInConversation(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        
        try {
            List<Message> messages = messageService.getAllMessagesInConversation(conversationId, userId);
            return ResponseEntity.ok(ApiResponse.success(messages));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 获取单条消息
     * GET /messages/{messageId}
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Message>> getMessageById(@PathVariable Long messageId) {
        try {
            Message message = messageService.getMessageById(messageId);
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 标记消息为已读
     * PATCH /messages/{messageId}/read?userId={userId}
     */
    @PatchMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<Integer>> markMessageAsRead(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        
        try {
            int updated = messageService.markMessageAsRead(messageId, userId);
            if (updated > 0) {
                return ResponseEntity.ok(ApiResponse.success(updated));
            } else {
                return ResponseEntity.ok(ApiResponse.success(0));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 标记与特定用户的所有消息为已读
     * PATCH /messages/read/all?userId={userId}&senderId={senderId}
     */
    @PatchMapping("/read/all")
    public ResponseEntity<ApiResponse<Integer>> markAllMessagesAsReadWithUser(
            @RequestParam Long userId,
            @RequestParam Long senderId) {
        
        try {
            int updated = messageService.markAllMessagesAsReadWithUser(userId, senderId);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 标记对话中的所有消息为已读
     * PATCH /messages/read/conversation/{conversationId}?userId={userId}
     */
    @PatchMapping("/read/conversation/{conversationId}")
    public ResponseEntity<ApiResponse<Integer>> markAllMessagesAsReadInConversation(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        
        try {
            int updated = messageService.markAllMessagesAsReadInConversation(conversationId, userId);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 获取用户未读消息总数
     * GET /messages/unread/count/{userId}
     */
    @GetMapping("/unread/count/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getTotalUnreadCount(@PathVariable Long userId) {
        try {
            long count = messageService.getTotalUnreadCount(userId);
            Map<String, Long> response = new HashMap<>();
            response.put("unreadCount", count);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 获取用户与特定用户的未读消息数
     * GET /messages/unread/between?userId={userId}&otherUserId={otherUserId}
     */
    @GetMapping("/unread/between")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCountWithUser(
            @RequestParam Long userId,
            @RequestParam Long otherUserId) {
        
        try {
            long count = messageService.getUnreadCountWithUser(userId, otherUserId);
            Map<String, Long> response = new HashMap<>();
            response.put("unreadCount", count);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 获取用户接收的最新消息
     * GET /messages/received/{userId}?page=0&size=10
     */
    @GetMapping("/received/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestReceivedMessages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            List<Message> messages = messageService.getLatestReceivedMessages(userId, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", messages);
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 获取用户发送的最新消息
     * GET /messages/sent/{userId}?page=0&size=10
     */
    @GetMapping("/sent/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestSentMessages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            List<Message> messages = messageService.getLatestSentMessages(userId, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", messages);
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 获取用户所有相关消息（按时间倒序）
     * GET /messages/all/{userId}?page=0&size=10
     */
    @GetMapping("/all/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUserMessages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            List<Message> messages = messageService.getAllUserMessages(userId, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", messages);
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 获取按对话分组的最新消息列表（类似微信首页）
     * GET /messages/latest/grouped/{userId}?page=0&size=10
     */
    @GetMapping("/latest/grouped/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestMessagesGroupedByConversation(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            List<MessageService.LatestConversationMessageDto> messages = 
                messageService.getLatestMessagesGroupedByConversation(userId, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", messages);
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 删除消息
     * DELETE /messages/{messageId}?userId={userId}
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        
        try {
            messageService.deleteMessage(messageId, userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }
}