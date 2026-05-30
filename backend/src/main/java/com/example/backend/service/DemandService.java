package com.example.backend.service;

import com.example.backend.entity.Demand;
import com.example.backend.entity.User;
import com.example.backend.repository.DemandRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DemandService {

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private UserRepository userRepository;

    // 通过ID获取Demand
    public Optional<Demand> getDemandById(Long id) {
        return demandRepository.findById(id);
    }

    // 多条件组合搜索筛选分页查找（增强版：支持标题、内容、作者昵称搜索）
    public Page<Demand> searchDemands(Long publisherId, String category, String status,
                                       String location, LocalDateTime deadlineStart,
                                       LocalDateTime deadlineEnd, Double minReward,
                                       Double maxReward, String keyword, String authorNickname,
                                       Pageable pageable) {
        Specification<Demand> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 发布者ID精确匹配
            if (publisherId != null) {
                predicates.add(cb.equal(root.get("publisherId"), publisherId));
            }
            
            // 分类精确匹配
            if (category != null && !category.isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            
            // 状态精确匹配
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            // 位置模糊搜索
            if (location != null && !location.isEmpty()) {
                predicates.add(cb.like(root.get("location"), "%" + location + "%"));
            }
            
            // 截止时间范围
            if (deadlineStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deadline"), deadlineStart));
            }
            if (deadlineEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deadline"), deadlineEnd));
            }
            
            // 奖励金额范围
            if (minReward != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reward"), minReward));
            }
            if (maxReward != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reward"), maxReward));
            }
            
            // 关键词搜索（标题或描述）
            if (keyword != null && !keyword.isEmpty()) {
                Predicate titleLike = cb.like(root.get("title"), "%" + keyword + "%");
                Predicate descLike = cb.like(root.get("description"), "%" + keyword + "%");
                predicates.add(cb.or(titleLike, descLike));
            }
            
            // 作者昵称搜索（关联User表）
            if (authorNickname != null && !authorNickname.isEmpty()) {
                // 使用子查询或JOIN查询用户表
                var subquery = query.subquery(Long.class);
                var subRoot = subquery.from(User.class);
                subquery.select(subRoot.get("id"))
                        .where(cb.like(subRoot.get("name"), "%" + authorNickname + "%"));
                predicates.add(root.get("publisherId").in(subquery));
            }
            
            // 排除已删除的状态
            predicates.add(cb.notEqual(root.get("status"), "DELETED"));
            
            // 去重（因为关联查询可能导致重复）
            query.distinct(true);
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return demandRepository.findAll(spec, pageable);
    }

    // 简化版搜索（支持标题/内容和作者昵称）
    public Page<Demand> searchDemandsSimple(Long publisherId, String category, String status, 
                                             String keyword, String authorNickname, Pageable pageable) {
        Specification<Demand> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (publisherId != null) {
                predicates.add(cb.equal(root.get("publisherId"), publisherId));
            }
            if (category != null && !category.isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            // 关键词搜索（标题或描述）
            if (keyword != null && !keyword.isEmpty()) {
                Predicate titleLike = cb.like(root.get("title"), "%" + keyword + "%");
                Predicate descLike = cb.like(root.get("description"), "%" + keyword + "%");
                predicates.add(cb.or(titleLike, descLike));
            }
            
            // 作者昵称搜索
            if (authorNickname != null && !authorNickname.isEmpty()) {
                var subquery = query.subquery(Long.class);
                var subRoot = subquery.from(User.class);
                subquery.select(subRoot.get("id"))
                        .where(cb.like(subRoot.get("name"), "%" + authorNickname + "%"));
                predicates.add(root.get("publisherId").in(subquery));
            }
            
            predicates.add(cb.notEqual(root.get("status"), "DELETED"));
            
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return demandRepository.findAll(spec, pageable);
    }

    // 更改需求状态
    @Transactional
    public boolean updateDemandStatus(Long id, String newStatus) {
        Optional<Demand> demandOpt = demandRepository.findById(id);
        if (demandOpt.isPresent()) {
            Demand demand = demandOpt.get();
            if (isValidStatusTransition(demand.getStatus(), newStatus)) {
                int updated = demandRepository.updateStatus(id, newStatus, LocalDateTime.now());
                return updated > 0;
            }
        }
        return false;
    }

    // 重置需求状态()（管理员操作 + 普通用户回退操作）
    @Transactional
    public boolean resetStatus(Long id, String newStatus) {
        Optional<Demand> demandOpt = demandRepository.findById(id);
        if (demandOpt.isPresent()) {
            Demand demand = demandOpt.get();
            int updated = demandRepository.updateStatus(id, newStatus, LocalDateTime.now());
            return updated > 0;
        }
        return false;
    }



    // 发布需求
    @Transactional
    public Demand publishDemand(Demand demand) {
        demand.setStatus("PENDING");
        demand.setCreatedAt(LocalDateTime.now());
        demand.setUpdatedAt(LocalDateTime.now());
        return demandRepository.save(demand);
    }

    // 编辑已存在的需求
    @Transactional
    public Optional<Demand> editDemand(Long id, Demand updatedDemand, Long publisherId) {
        Optional<Demand> existingOpt = demandRepository.findById(id);
        
        if (existingOpt.isPresent()) {
            Demand existing = existingOpt.get();
            
            if (existing.getPublisherId().equals(publisherId) && 
                ("PENDING".equals(existing.getStatus()) || "REJECTED".equals(existing.getStatus()))) {
                
                if (updatedDemand.getTitle() != null) existing.setTitle(updatedDemand.getTitle());
                if (updatedDemand.getDescription() != null) existing.setDescription(updatedDemand.getDescription());
                if (updatedDemand.getCategory() != null) existing.setCategory(updatedDemand.getCategory());
                if (updatedDemand.getLocation() != null) existing.setLocation(updatedDemand.getLocation());
                if (updatedDemand.getDeadline() != null) existing.setDeadline(updatedDemand.getDeadline());
                if (updatedDemand.getReward() != null) existing.setReward(updatedDemand.getReward());
                if (updatedDemand.getPictureUrls() != null) existing.setPictureUrls(updatedDemand.getPictureUrls());
                
                existing.setUpdatedAt(LocalDateTime.now());
                return Optional.of(demandRepository.save(existing));
            }
        }
        return Optional.empty();
    }

    // 删除需求（软删除）
    @Transactional
    public boolean deleteDemand(Long id, Long publisherId) {
        Optional<Demand> demandOpt = demandRepository.findById(id);
        if (demandOpt.isPresent()) {
            Demand demand = demandOpt.get();
            if (demand.getPublisherId().equals(publisherId) && "PENDING".equals(demand.getStatus())) {
                demand.setStatus("DELETED");
                demand.setUpdatedAt(LocalDateTime.now());
                demandRepository.save(demand);
                return true;
            }
        }
        return false;
    }

    // 获取某个发布者的所有需求
    public Page<Demand> getDemandsByPublisher(Long publisherId, Pageable pageable) {
        return demandRepository.findByPublisherId(publisherId, pageable);
    }

    // 获取所有进行中的需求
    public Page<Demand> getActiveDemands(Pageable pageable) {
        Specification<Demand> spec = (root, query, cb) -> {
            Predicate pending = cb.equal(root.get("status"), "PENDING");
            Predicate accepted = cb.equal(root.get("status"), "ACCEPTED");
            return cb.or(pending, accepted);
        };
        return demandRepository.findAll(spec, pageable);
    }

    // 更新订单ID
    @Transactional
    public boolean updateOrderId(Long demandId, Long orderId) {
        int updated = demandRepository.updateOrderId(demandId, orderId);
        return updated > 0;
    }

    // 根据发布者ID获取用户昵称
    public String getPublisherNickname(Long publisherId) {
        return userRepository.findById(publisherId)
                .map(User::getName)
                .orElse("未知用户");
    }

    // 批量获取需求对应的发布者昵称（用于列表展示）
    public Map<Long, String> getPublisherNicknames(List<Demand> demands) {
        Set<Long> publisherIds = demands.stream()
                .map(Demand::getPublisherId)
                .collect(Collectors.toSet());
        
        List<User> users = userRepository.findAllById(publisherIds);
        return users.stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
    }

    // 验证状态转换是否合法
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        return switch (currentStatus) {
            case "PENDING" -> Set.of("ACCEPTED", "CANCELLED", "EXPIRED", "DELETED").contains(newStatus);
            case "ACCEPTED" -> Set.of("COMPLETED", "CANCELLED").contains(newStatus);
            case "REJECTED" -> Set.of("PENDING", "DELETED").contains(newStatus);
            case "COMPLETED", "CANCELLED", "EXPIRED", "DELETED" -> false;
            default -> false;
        };
    }
}