package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.entity.Demand;
import com.example.backend.service.DemandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/demands")
@CrossOrigin(origins = "*")
public class DemandController {

    @Autowired
    private DemandService demandService;

    // 通过ID获取整个Demand信息
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Demand>> getDemandById(@PathVariable Long id) {
        Optional<Demand> demandOpt = demandService.getDemandById(id);
        if (demandOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(demandOpt.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "需求不存在"));
        }
    }

    // 多条件组合搜索筛选分页查找（增强版：支持标题、内容、作者昵称）
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchDemands(
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineEnd,
            @RequestParam(required = false) Double minReward,
            @RequestParam(required = false) Double maxReward,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String authorNickname,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Demand> demandPage = demandService.searchDemands(publisherId, category, status, 
                location, deadlineStart, deadlineEnd, minReward, maxReward, keyword, authorNickname, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", demandPage.getContent());
        response.put("totalElements", demandPage.getTotalElements());
        response.put("totalPages", demandPage.getTotalPages());
        response.put("currentPage", demandPage.getNumber());
        response.put("pageSize", demandPage.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 简化版搜索（支持标题/内容和作者昵称）
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> filterDemands(
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String authorNickname,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Demand> demandPage = demandService.searchDemandsSimple(publisherId, category, status, keyword, authorNickname, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", demandPage.getContent());
        response.put("totalElements", demandPage.getTotalElements());
        response.put("totalPages", demandPage.getTotalPages());
        response.put("currentPage", demandPage.getNumber());
        response.put("pageSize", demandPage.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 更改需求状态
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateDemandStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        
        if (!isValidStatus(status)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "无效的状态值，有效值为：PENDING, ACCEPTED, REJECTED, COMPLETED, CANCELLED, EXPIRED"));
        }
        
        boolean updated = demandService.updateDemandStatus(id, status);
        if (updated) {
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "状态更新失败，请检查需求是否存在或状态转换是否合法"));
        }
    }

    // 发布需求
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Demand>> publishDemand(@RequestBody Demand demand) {
        if (demand.getTitle() == null || demand.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "需求标题不能为空"));
        }
        if (demand.getCategory() == null || demand.getCategory().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "需求分类不能为空"));
        }
        if (demand.getPublisherId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "发布者ID不能为空"));
        }
        
        Demand savedDemand = demandService.publishDemand(demand);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(savedDemand));
    }

    // 编辑已存在的需求
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Demand>> editDemand(
            @PathVariable Long id,
            @RequestBody Demand demand,
            @RequestParam Long publisherId) {
        
        Optional<Demand> updatedOpt = demandService.editDemand(id, demand, publisherId);
        if (updatedOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(updatedOpt.get()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "编辑失败，请确认您是发布者且需求状态为PENDING或REJECTED"));
        }
    }

    // 删除需求（软删除）
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDemand(
            @PathVariable Long id,
            @RequestParam Long publisherId) {
        
        boolean deleted = demandService.deleteDemand(id, publisherId);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "删除失败，请确认您是发布者且需求状态为PENDING"));
        }
    }

    // 获取某个发布者的所有需求
    @GetMapping("/publisher/{publisherId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDemandsByPublisher(
            @PathVariable Long publisherId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Demand> demandPage = demandService.getDemandsByPublisher(publisherId, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", demandPage.getContent());
        response.put("totalElements", demandPage.getTotalElements());
        response.put("totalPages", demandPage.getTotalPages());
        response.put("currentPage", demandPage.getNumber());
        response.put("publisherNickname", demandService.getPublisherNickname(publisherId));
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 获取进行中的需求
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveDemands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Demand> demandPage = demandService.getActiveDemands(pageable);

        
        Map<String, Object> response = new HashMap<>();
        response.put("content", demandPage.getContent());
        response.put("totalElements", demandPage.getTotalElements());
        response.put("totalPages", demandPage.getTotalPages());
        response.put("currentPage", demandPage.getNumber());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 获取所有可用的分类
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<String[]>> getCategories() {
        String[] categories = {"生活服务", "专业技术", "教育培训", "设计创意", "文案写作", "数据标注", "翻译服务", "其他"};
        return ResponseEntity.ok(ApiResponse.success(categories));
    }


    // 辅助方法：验证状态值
    private boolean isValidStatus(String status) {
        return Set.of("PENDING", "ACCEPTED", "REJECTED", "COMPLETED", "CANCELLED", "EXPIRED")
                .contains(status);
    }
}