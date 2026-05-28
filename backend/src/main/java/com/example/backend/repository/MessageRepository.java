package com.example.backend.repository;

import com.example.backend.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * 获取对话中的所有消息（按时间正序）
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    
    /**
     * 获取对话中的消息（分页，按时间倒序）
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId ORDER BY m.createdAt DESC")
    List<Message> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") Long conversationId, Pageable pageable);
    
    /**
     * 获取用户未读消息数量
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :userId AND m.isRead = false")
    long countUnreadMessagesByUserId(@Param("userId") Long userId);
    
    /**
     * 获取用户与特定对话方的未读消息数量
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :userId AND m.senderId = :senderId AND m.isRead = false")
    long countUnreadMessagesBetweenUsers(@Param("userId") Long userId, @Param("senderId") Long senderId);
    
    /**
     * 获取用户接收的最新消息（按时间倒序）
     */
    List<Message> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);
    
    /**
     * 获取用户发送的最新消息
     */
    List<Message> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);
    
    /**
     * 获取用户所有相关消息（发送或接收）
     */
    @Query("SELECT m FROM Message m WHERE m.senderId = :userId OR m.receiverId = :userId ORDER BY m.createdAt DESC")
    List<Message> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 获取两个用户之间的所有消息（按时间正序）
     */
    @Query("SELECT m FROM Message m WHERE (m.senderId = :user1Id AND m.receiverId = :user2Id) OR (m.senderId = :user2Id AND m.receiverId = :user1Id) ORDER BY m.createdAt ASC")
    List<Message> findMessagesBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
    
    /**
     * 标记消息为已读
     */
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.id = :messageId AND m.receiverId = :userId")
    int markMessageAsRead(@Param("messageId") Long messageId, @Param("userId") Long userId);
    
    /**
     * 标记用户与特定发送者之间的所有消息为已读
     */
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiverId = :userId AND m.senderId = :senderId AND m.isRead = false")
    int markAllMessagesAsReadBetweenUsers(@Param("userId") Long userId, @Param("senderId") Long senderId);
    
    /**
     * 标记对话中的所有消息为已读
     */
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.receiverId = :userId AND m.isRead = false")
    int markAllMessagesAsReadInConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
    
    /**
     * 获取对话中的未读消息数量
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId AND m.receiverId = :userId AND m.isRead = false")
    long countUnreadMessagesInConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
    
    /**
     * 删除对话中的所有消息
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.conversationId = :conversationId")
    void deleteAllByConversationId(@Param("conversationId") Long conversationId);
    
    /**
     * 获取用户最近的消息列表（按对话分组）
     */
    @Query("SELECT m FROM Message m WHERE m.id IN (" +
           "SELECT MAX(m2.id) FROM Message m2 WHERE m2.senderId = :userId OR m2.receiverId = :userId GROUP BY " +
           "CASE WHEN m2.senderId = :userId THEN m2.receiverId ELSE m2.senderId END) " +
           "ORDER BY m.createdAt DESC")
    List<Message> findLatestMessagesByUser(@Param("userId") Long userId, Pageable pageable);
}