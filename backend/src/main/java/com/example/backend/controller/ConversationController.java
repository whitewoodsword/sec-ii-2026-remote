package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.entity.Message;
import com.example.backend.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    /**
     * 获取或创建对话
     * POST /conversations?user1Id={user1Id}&user2Id={user2Id}
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationService.ConversationDto>> getOrCreateConversation(
            @RequestParam Long user1Id,
            @RequestParam Long user2Id) {
        
        try {
            conversationService.getOrCreateConversation(user1Id, user2Id);
            // 获取转换后的DTO
            List<ConversationService.ConversationDto> conversations = 
                conversationService.getUserConversations(user1Id, 0, 100);
            
            ConversationService.ConversationDto targetConv = conversations.stream()
                    .filter(c -> c.getOtherUser().getId().equals(user2Id))
                    .findFirst()
                    .orElse(null);
            
            return ResponseEntity.ok(ApiResponse.success(targetConv));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 获取用户的对话列表
     * GET /conversations/user/{userId}?page=0&size=10
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserConversations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            List<ConversationService.ConversationDto> conversations = 
                conversationService.getUserConversations(userId, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", conversations);
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 获取对话详情（包含完整消息历史）
     * GET /conversations/{conversationId}?userId={userId}
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationService.ConversationDetailDto>> getConversationDetail(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        
        try {
            ConversationService.ConversationDetailDto detail = 
                conversationService.getConversationDetail(conversationId, userId);
            return ResponseEntity.ok(ApiResponse.success(detail));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * 获取两个用户之间的消息历史
     * GET /conversations/between?userId={userId}&otherUserId={otherUserId}
     */
    @GetMapping("/between")
    public ResponseEntity<ApiResponse<List<Message>>> getMessagesBetweenUsers(
            @RequestParam Long userId,
            @RequestParam Long otherUserId) {
        
        try {
            List<Message> messages = conversationService.getMessagesBetweenUsers(userId, otherUserId);
            return ResponseEntity.ok(ApiResponse.success(messages));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 删除对话
     * DELETE /conversations/{conversationId}?userId={userId}
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        
        try {
            conversationService.deleteConversation(conversationId, userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }
}