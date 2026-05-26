package com.example.backend.repository;

import com.example.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 根据接收者ID查询所有消息（按时间倒序，最新的在前面）
    List<Message> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    // 查询某用户的未读消息数量
    long countByReceiverIdAndIsReadFalse(Long receiverId);

    // 查询某用户的所有未读消息
    List<Message> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(Long receiverId);

    // 将某用户的所有消息标记为已读
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiverId = :receiverId")
    void markAllAsRead(@Param("receiverId") Long receiverId);

    // 将单条消息标记为已读
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.id = :messageId")
    void markAsRead(@Param("messageId") Long messageId);

    // 获取两个用户之间的私信对话（按时间正序）
    @Query("SELECT m FROM Message m WHERE m.type = 'CHAT' AND " +
           "((m.senderId = :user1 AND m.receiverId = :user2) OR " +
           "(m.senderId = :user2 AND m.receiverId = :user1)) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("user1") Long user1, @Param("user2") Long user2);

    // 按类型获取某用户的消息
    List<Message> findByReceiverIdAndTypeOrderByCreatedAtDesc(Long receiverId, String type);

    // 获取某用户的所有对话伙伴 ID
    @Query("SELECT DISTINCT m.senderId FROM Message m WHERE m.receiverId = :userId AND m.type = 'CHAT'")
    List<Long> findChatSenderIds(@Param("userId") Long userId);

    @Query("SELECT DISTINCT m.receiverId FROM Message m WHERE m.senderId = :userId AND m.type = 'CHAT'")
    List<Long> findChatReceiverIds(@Param("userId") Long userId);

    // 获取两人之间的最后一条消息
    @Query("SELECT m FROM Message m WHERE m.type = 'CHAT' AND m.createdAt = " +
           "(SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.type = 'CHAT' AND " +
           "((m2.senderId = :user1 AND m2.receiverId = :user2) OR " +
           "(m2.senderId = :user2 AND m2.receiverId = :user1)))")
    java.util.Optional<Message> findLastMessageBetween(@Param("user1") Long user1, @Param("user2") Long user2);

    // 某发送者给某接收者的未读消息数
    long countBySenderIdAndReceiverIdAndIsReadFalse(Long senderId, Long receiverId);

    // 按类型统计某用户的未读消息数
    long countByReceiverIdAndTypeAndIsReadFalse(Long receiverId, String type);

    // 将某人对某用户的未读消息全部标记为已读
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.senderId = :senderId AND m.receiverId = :receiverId AND m.isRead = false")
    void markConversationAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}