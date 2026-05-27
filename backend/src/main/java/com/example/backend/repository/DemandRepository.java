package com.example.backend.repository;
import com.example.backend.entity.Demand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.LocalDateTime;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long>, JpaSpecificationExecutor<Demand> {

    Page<Demand> findByPublisherId(Long publisherId, Pageable pageable);
    
    Page<Demand> findByStatus(String status, Pageable pageable);
    
    Page<Demand> findByCategory(String category, Pageable pageable);
    
    @Modifying
    @Transactional
    @Query("UPDATE Demand d SET d.status = :status, d.updatedAt = :now WHERE d.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE Demand d SET d.orderId = :orderId WHERE d.id = :id")
    int updateOrderId(@Param("id") Long id, @Param("orderId") Long orderId);
    
    boolean existsByIdAndPublisherId(Long id, Long publisherId); 
    List<Demand> findAllByOrderByCreatedAtDesc();
}

