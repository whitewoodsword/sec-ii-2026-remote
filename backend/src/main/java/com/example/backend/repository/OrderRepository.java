package com.example.backend.repository;

import com.example.backend.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /**
     * 根据需求ID查找订单
     */
    Optional<Order> findByDemandId(Long demandId);

    /**
     * 根据发布者ID分页查询订单（作为服务需求方）
     */
    Page<Order> findByPublisherId(Long publisherId, Pageable pageable);

    /**
     * 根据接单者ID分页查询订单（作为服务提供方）
     */
    Page<Order> findByAcceptorId(Long acceptorId, Pageable pageable);

    /**
     * 根据用户ID查询所有相关订单（作为发布者或接单者）
     */
    @Query("SELECT o FROM Order o WHERE o.publisherId = :userId OR o.acceptorId = :userId")
    Page<Order> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 更新订单状态
     */
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status, o.updatedAt = :updatedAt WHERE o.id = :orderId")
    int updateOrderStatus(@Param("orderId") Long orderId, 
                          @Param("status") String status, 
                          @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 完成订单（设置完成时间）
     */
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = 'COMPLETED', o.completedAt = :completedAt, o.updatedAt = :updatedAt WHERE o.id = :orderId")
    int completeOrder(@Param("orderId") Long orderId, 
                      @Param("completedAt") LocalDateTime completedAt,
                      @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 更新订单的备注信息
     */
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.latestRequesterNote = :note, o.updatedAt = :updatedAt WHERE o.id = :orderId")
    int updateOrderNote(@Param("orderId") Long orderId, 
                        @Param("note") String note,
                        @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 更新订单的评价ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.commentId = :commentId, o.updatedAt = :updatedAt WHERE o.id = :orderId")
    int updateCommentId(@Param("orderId") Long orderId, 
                        @Param("commentId") Long commentId,
                        @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 检查用户是否与该订单相关（作为发布者或接单者）
     */
    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.id = :orderId AND (o.publisherId = :userId OR o.acceptorId = :userId)")
    boolean isUserRelatedToOrder(@Param("orderId") Long orderId, @Param("userId") Long userId);

    /**
     * 获取用户作为发布者的订单数量
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.publisherId = :userId")
    long countByPublisherId(@Param("userId") Long userId);

    /**
     * 获取用户作为接单者的订单数量
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.acceptorId = :userId")
    long countByAcceptorId(@Param("userId") Long userId);

    /**
     * 根据状态查询订单
     */
    Page<Order> findByStatus(String status, Pageable pageable);

    /**
     * 获取用户特定状态的订单（作为发布者）
     */
    Page<Order> findByPublisherIdAndStatus(Long publisherId, String status, Pageable pageable);

    /**
     * 获取用户特定状态的订单（作为接单者）
     */
    Page<Order> findByAcceptorIdAndStatus(Long acceptorId, String status, Pageable pageable);
}