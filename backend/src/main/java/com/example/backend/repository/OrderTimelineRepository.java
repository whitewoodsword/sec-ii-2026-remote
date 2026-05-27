package com.example.backend.repository;

import com.example.backend.entity.OrderTimelineRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTimelineRepository extends JpaRepository<OrderTimelineRecord, Long> {
    List<OrderTimelineRecord> findByOrderIdOrderByHappenedAtAsc(Long orderId);

    void deleteByOrderId(Long orderId);
}
