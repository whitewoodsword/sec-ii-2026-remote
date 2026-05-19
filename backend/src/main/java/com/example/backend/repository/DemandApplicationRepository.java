package com.example.backend.repository;

import com.example.backend.entity.DemandApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandApplicationRepository extends JpaRepository<DemandApplication, Long> {
    List<DemandApplication> findByDemandIdOrderByCreatedAtAsc(Long demandId);
}
