package com.example.backend.repository;

import com.example.backend.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * 查找两个用户之间的对话
     */
    @Query("SELECT c FROM Conversation c WHERE (c.user1Id = :user1Id AND c.user2Id = :user2Id) OR (c.user1Id = :user2Id AND c.user2Id = :user1Id)")
    Optional<Conversation> findConversationBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
    
    /**
     * 获取用户的所有对话（按最后消息时间倒序）
     */
    @Query("SELECT c FROM Conversation c WHERE c.user1Id = :userId OR c.user2Id = :userId ORDER BY c.lastMessageTime DESC")
    List<Conversation> findAllByUserIdOrderByLastMessageTimeDesc(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 获取用户的所有对话（分页）
     */
    @Query("SELECT c FROM Conversation c WHERE c.user1Id = :userId OR c.user2Id = :userId")
    List<Conversation> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 统计用户的对话数量
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.user1Id = :userId OR c.user2Id = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * 更新对话的最后消息ID和时间
     */
    @Modifying
    @Transactional
    @Query("UPDATE Conversation c SET c.lastMessageId = :messageId, c.lastMessageTime = :messageTime WHERE c.id = :conversationId")
    void updateLastMessage(@Param("conversationId") Long conversationId, 
                          @Param("messageId") Long messageId, 
                          @Param("messageTime") java.time.LocalDateTime messageTime);
    
    /**
     * 删除用户的所有对话
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Conversation c WHERE c.user1Id = :userId OR c.user2Id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}