package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.backend.entity.Demand;
import com.example.backend.entity.User;
import com.example.backend.repository.DemandRepository;
import com.example.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("需求服务单元测试")
class DemandServiceTests {

    @Mock
    private DemandRepository demandRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DemandService demandService;

    private Demand testDemand;
    private User testUser;
    private final Long TEST_DEMAND_ID = 1L;
    private final Long TEST_PUBLISHER_ID = 100L;
    private final Long TEST_ORDER_ID = 200L;
    private final String TEST_PICTURE_URLS = "url1,url2,url3";

    @BeforeEach
    void setUp() {
        testUser = new User("测试用户", "13800138000", "password");
        testUser.setId(TEST_PUBLISHER_ID);

        testDemand = new Demand();
        testDemand.setId(TEST_DEMAND_ID);
        testDemand.setTitle("测试需求标题");
        testDemand.setDescription("这是一个测试需求的详细描述");
        testDemand.setCategory("IT技术支持");
        testDemand.setPublisherId(TEST_PUBLISHER_ID);
        testDemand.setStatus("PENDING");
        testDemand.setLocation("上海市浦东新区");
        testDemand.setDeadline(LocalDateTime.now().plusDays(7));
        testDemand.setReward(500.0);
        testDemand.setCreatedAt(LocalDateTime.now());
        testDemand.setUpdatedAt(LocalDateTime.now());
        testDemand.setOrderId(null);
        testDemand.setPictureUrls(TEST_PICTURE_URLS);
    }

    // ==================== 需求发布测试 ====================

    @Nested
    @DisplayName("需求发布测试")
    class PublishDemandTests {

        @Test
        @DisplayName("成功发布需求")
        void testPublishDemandSuccess() {
            // Given
            Demand newDemand = new Demand(
                "新需求", "描述", "设计", TEST_PUBLISHER_ID,
                "北京", LocalDateTime.now().plusDays(5), 300.0, "pic_url"
            );
            when(demandRepository.save(any(Demand.class))).thenAnswer(invocation -> {
                Demand saved = invocation.getArgument(0);
                saved.setId(TEST_DEMAND_ID);
                return saved;
            });

            // When
            Demand result = demandService.publishDemand(newDemand);

            // Then
            assertNotNull(result);
            assertEquals("PENDING", result.getStatus());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
            verify(demandRepository).save(any(Demand.class));
        }

        @Test
        @DisplayName("发布需求时自动设置创建和更新时间")
        void testPublishDemandSetsTimestamps() {
            // Given
            Demand newDemand = new Demand();
            newDemand.setTitle("定时测试");
            when(demandRepository.save(any(Demand.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Demand result = demandService.publishDemand(newDemand);

            // Then
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
            assertEquals("PENDING", result.getStatus());
        }
    }

    // ==================== 需求查询测试 ====================

    @Nested
    @DisplayName("需求查询测试")
    class DemandQueryTests {

        @Test
        @DisplayName("根据ID获取需求")
        void testGetDemandById() {
            // Given
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));

            // When
            Optional<Demand> result = demandService.getDemandById(TEST_DEMAND_ID);

            // Then
            assertTrue(result.isPresent());
            assertEquals(TEST_DEMAND_ID, result.get().getId());
            assertEquals("测试需求标题", result.get().getTitle());
        }

        @Test
        @DisplayName("获取不存在需求返回空")
        void testGetDemandByIdNotFound() {
            // Given
            when(demandRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<Demand> result = demandService.getDemandById(999L);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("根据发布者ID获取需求分页")
        void testGetDemandsByPublisher() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Demand> page = new PageImpl<>(List.of(testDemand), pageable, 1);
            when(demandRepository.findByPublisherId(TEST_PUBLISHER_ID, pageable)).thenReturn(page);

            // When
            Page<Demand> result = demandService.getDemandsByPublisher(TEST_PUBLISHER_ID, pageable);

            // Then
            assertEquals(1, result.getTotalElements());
            assertEquals(TEST_DEMAND_ID, result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("获取进行中的需求（PENDING和ACCEPTED状态）")
        void testGetActiveDemands() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Demand> page = new PageImpl<>(List.of(testDemand), pageable, 1);
            when(demandRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Demand> result = demandService.getActiveDemands(pageable);

            // Then
            assertNotNull(result);
            verify(demandRepository).findAll(any(Specification.class), eq(pageable));
        }
    }

    // ==================== 需求编辑测试 ====================

    @Nested
    @DisplayName("需求编辑测试")
    class EditDemandTests {

        @Test
        @DisplayName("成功编辑PENDING状态的需求")
        void testEditPendingDemandSuccess() {
            // Given
            Demand updatedInfo = new Demand();
            updatedInfo.setTitle("修改后的标题");
            updatedInfo.setDescription("修改后的描述");
            updatedInfo.setReward(1000.0);
            updatedInfo.setLocation("北京市朝阳区");

            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(demandRepository.save(any(Demand.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Optional<Demand> result = demandService.editDemand(TEST_DEMAND_ID, updatedInfo, TEST_PUBLISHER_ID);

            // Then
            assertTrue(result.isPresent());
            assertEquals("修改后的标题", result.get().getTitle());
            assertEquals("修改后的描述", result.get().getDescription());
            assertEquals(1000.0, result.get().getReward());
            assertEquals("北京市朝阳区", result.get().getLocation());
            assertNotNull(result.get().getUpdatedAt());
        }

        @Test
        @DisplayName("成功编辑REJECTED状态的需求")
        void testEditRejectedDemandSuccess() {
            // Given
            testDemand.setStatus("REJECTED");
            Demand updatedInfo = new Demand();
            updatedInfo.setTitle("重新编辑被拒绝的需求");

            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(demandRepository.save(any(Demand.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Optional<Demand> result = demandService.editDemand(TEST_DEMAND_ID, updatedInfo, TEST_PUBLISHER_ID);

            // Then
            assertTrue(result.isPresent());
            assertEquals("重新编辑被拒绝的需求", result.get().getTitle());
        }

        @Test
        @DisplayName("不能编辑ACCEPTED状态的需求")
        void testCannotEditAcceptedDemand() {
            // Given
            testDemand.setStatus("ACCEPTED");
            Demand updatedInfo = new Demand();
            updatedInfo.setTitle("尝试修改已接受的需求");

            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));

            // When
            Optional<Demand> result = demandService.editDemand(TEST_DEMAND_ID, updatedInfo, TEST_PUBLISHER_ID);

            // Then
            assertTrue(result.isEmpty());
            verify(demandRepository, never()).save(any());
        }

        @Test
        @DisplayName("非发布者不能编辑需求")
        void testNonPublisherCannotEdit() {
            // Given
            Demand updatedInfo = new Demand();
            updatedInfo.setTitle("非法编辑");
            Long wrongPublisherId = 999L;

            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));

            // When
            Optional<Demand> result = demandService.editDemand(TEST_DEMAND_ID, updatedInfo, wrongPublisherId);

            // Then
            assertTrue(result.isEmpty());
            verify(demandRepository, never()).save(any());
        }

        @Test
        @DisplayName("编辑不存在需求返回空")
        void testEditNonExistentDemand() {
            // Given
            when(demandRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<Demand> result = demandService.editDemand(999L, new Demand(), TEST_PUBLISHER_ID);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("部分更新需求字段")
        void testPartialUpdateDemand() {
            // Given
            Demand partialUpdate = new Demand();
            partialUpdate.setTitle("只更新标题");
            // 其他字段为null

            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(demandRepository.save(any(Demand.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Optional<Demand> result = demandService.editDemand(TEST_DEMAND_ID, partialUpdate, TEST_PUBLISHER_ID);

            // Then
            assertTrue(result.isPresent());
            assertEquals("只更新标题", result.get().getTitle());
            assertEquals("这是一个测试需求的详细描述", result.get().getDescription()); // 保持原值
            assertEquals(500.0, result.get().getReward()); // 保持原值
        }
    }

    // ==================== 需求删除测试（软删除） ====================

    @Nested
    @DisplayName("需求删除测试")
    class DeleteDemandTests {

        @Test
        @DisplayName("成功删除PENDING状态的需求")
        void testDeletePendingDemandSuccess() {
            // Given
            testDemand.setStatus("PENDING");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(demandRepository.save(any(Demand.class))).thenReturn(testDemand);

            // When
            boolean result = demandService.deleteDemand(TEST_DEMAND_ID, TEST_PUBLISHER_ID);

            // Then
            assertTrue(result);
            assertEquals("DELETED", testDemand.getStatus());
            verify(demandRepository).save(testDemand);
        }

        @Test
        @DisplayName("不能删除非PENDING状态的需求")
        void testCannotDeleteNonPendingDemand() {
            // Given
            testDemand.setStatus("ACCEPTED");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));

            // When
            boolean result = demandService.deleteDemand(TEST_DEMAND_ID, TEST_PUBLISHER_ID);

            // Then
            assertFalse(result);
            verify(demandRepository, never()).save(any());
        }

        @Test
        @DisplayName("非发布者不能删除需求")
        void testNonPublisherCannotDelete() {
            // Given
            Long wrongPublisherId = 999L;
            testDemand.setStatus("PENDING");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));

            // When
            boolean result = demandService.deleteDemand(TEST_DEMAND_ID, wrongPublisherId);

            // Then
            assertFalse(result);
            verify(demandRepository, never()).save(any());
        }

        @Test
        @DisplayName("删除不存在需求返回false")
        void testDeleteNonExistentDemand() {
            // Given
            when(demandRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = demandService.deleteDemand(999L, TEST_PUBLISHER_ID);

            // Then
            assertFalse(result);
        }
    }

    // ==================== 需求状态更新测试 ====================

    @Nested
    @DisplayName("需求状态更新测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("PENDING -> ACCEPTED 合法转换")
        void testPendingToAccepted() {
            // Given
            testDemand.setStatus("PENDING");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(demandRepository.updateStatus(eq(TEST_DEMAND_ID), eq("ACCEPTED"), any(LocalDateTime.class)))
                .thenReturn(1);

            // When
            boolean result = demandService.updateDemandStatus(TEST_DEMAND_ID, "ACCEPTED");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("PENDING -> CANCELLED 合法转换")
        void testPendingToCancelled() {
            // Given
            testDemand.setStatus("PENDING");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(demandRepository.updateStatus(eq(TEST_DEMAND_ID), eq("CANCELLED"), any(LocalDateTime.class)))
                .thenReturn(1);

            // When
            boolean result = demandService.updateDemandStatus(TEST_DEMAND_ID, "CANCELLED");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("ACCEPTED -> COMPLETED 合法转换")
        void testAcceptedToCompleted() {
            // Given
            testDemand.setStatus("ACCEPTED");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(demandRepository.updateStatus(eq(TEST_DEMAND_ID), eq("COMPLETED"), any(LocalDateTime.class)))
                .thenReturn(1);

            // When
            boolean result = demandService.updateDemandStatus(TEST_DEMAND_ID, "COMPLETED");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("ACCEPTED -> CANCELLED 合法转换")
        void testAcceptedToCancelled() {
            // Given
            testDemand.setStatus("ACCEPTED");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(demandRepository.updateStatus(eq(TEST_DEMAND_ID), eq("CANCELLED"), any(LocalDateTime.class)))
                .thenReturn(1);

            // When
            boolean result = demandService.updateDemandStatus(TEST_DEMAND_ID, "CANCELLED");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("REJECTED -> PENDING 合法转换")
        void testRejectedToPending() {
            // Given
            testDemand.setStatus("REJECTED");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(demandRepository.updateStatus(eq(TEST_DEMAND_ID), eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(1);

            // When
            boolean result = demandService.updateDemandStatus(TEST_DEMAND_ID, "PENDING");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("COMPLETED 状态无法转换")
        void testCompletedCannotTransition() {
            // Given
            testDemand.setStatus("COMPLETED");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));

            // When
            boolean result = demandService.updateDemandStatus(TEST_DEMAND_ID, "CANCELLED");

            // Then
            assertFalse(result);
            verify(demandRepository, never()).updateStatus(any(), any(), any());
        }

        @Test
        @DisplayName("CANCELLED 状态无法转换")
        void testCancelledCannotTransition() {
            // Given
            testDemand.setStatus("CANCELLED");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));

            // When
            boolean result = demandService.updateDemandStatus(TEST_DEMAND_ID, "PENDING");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("非法状态转换返回false")
        void testInvalidStatusTransition() {
            // Given
            testDemand.setStatus("PENDING");
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));

            // When - PENDING不能直接转到COMPLETED
            boolean result = demandService.updateDemandStatus(TEST_DEMAND_ID, "COMPLETED");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("更新不存在需求的状态")
        void testUpdateStatusForNonExistentDemand() {
            // Given
            when(demandRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = demandService.updateDemandStatus(999L, "ACCEPTED");

            // Then
            assertFalse(result);
        }
    }

    // ==================== 重置状态测试 ====================

    @Nested
    @DisplayName("重置状态测试")
    class ResetStatusTests {

        @Test
        @DisplayName("管理员重置需求状态")
        void testResetStatusSuccess() {
            // Given
            when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
            when(demandRepository.updateStatus(eq(TEST_DEMAND_ID), eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(1);

            // When
            boolean result = demandService.resetStatus(TEST_DEMAND_ID, "PENDING");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("重置不存在需求的状态")
        void testResetStatusForNonExistentDemand() {
            // Given
            when(demandRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = demandService.resetStatus(999L, "PENDING");

            // Then
            assertFalse(result);
        }
    }

    // ==================== 订单关联测试 ====================

    @Nested
    @DisplayName("订单关联测试")
    class OrderAssociationTests {

        @Test
        @DisplayName("更新需求关联的订单ID")
        void testUpdateOrderIdSuccess() {
            // Given
            when(demandRepository.updateOrderId(TEST_DEMAND_ID, TEST_ORDER_ID)).thenReturn(1);

            // When
            boolean result = demandService.updateOrderId(TEST_DEMAND_ID, TEST_ORDER_ID);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("更新不存在的需求订单ID")
        void testUpdateOrderIdForNonExistentDemand() {
            // Given
            when(demandRepository.updateOrderId(999L, TEST_ORDER_ID)).thenReturn(0);

            // When
            boolean result = demandService.updateOrderId(999L, TEST_ORDER_ID);

            // Then
            assertFalse(result);
        }
    }

    // ==================== 用户昵称相关测试 ====================

    @Nested
    @DisplayName("用户昵称相关测试")
    class PublisherNicknameTests {

        @Test
        @DisplayName("根据发布者ID获取用户昵称")
        void testGetPublisherNickname() {
            // Given
            when(userRepository.findById(TEST_PUBLISHER_ID)).thenReturn(Optional.of(testUser));

            // When
            String nickname = demandService.getPublisherNickname(TEST_PUBLISHER_ID);

            // Then
            assertEquals("测试用户", nickname);
        }

        @Test
        @DisplayName("发布者不存在时返回默认昵称")
        void testGetPublisherNicknameWhenUserNotFound() {
            // Given
            when(userRepository.findById(TEST_PUBLISHER_ID)).thenReturn(Optional.empty());

            // When
            String nickname = demandService.getPublisherNickname(TEST_PUBLISHER_ID);

            // Then
            assertEquals("未知用户", nickname);
        }

        @Test
        @DisplayName("批量获取发布者昵称")
        void testGetPublisherNicknames() {
            // Given
            List<Demand> demands = List.of(testDemand);
            List<User> users = List.of(testUser);
            when(userRepository.findAllById(Set.of(TEST_PUBLISHER_ID))).thenReturn(users);

            // When
            Map<Long, String> nicknameMap = demandService.getPublisherNicknames(demands);

            // Then
            assertEquals(1, nicknameMap.size());
            assertEquals("测试用户", nicknameMap.get(TEST_PUBLISHER_ID));
        }

        @Test
        @DisplayName("批量获取时多个需求同一发布者只查一次")
        void testGetPublisherNicknamesDeduplication() {
            // Given
            Demand demand2 = new Demand();
            demand2.setId(2L);
            demand2.setPublisherId(TEST_PUBLISHER_ID);
            List<Demand> demands = List.of(testDemand, demand2);
            List<User> users = List.of(testUser);
            when(userRepository.findAllById(Set.of(TEST_PUBLISHER_ID))).thenReturn(users);

            // When
            Map<Long, String> nicknameMap = demandService.getPublisherNicknames(demands);

            // Then
            assertEquals(1, nicknameMap.size());
            // 验证只查询了一次，使用Set去重
            verify(userRepository, times(1)).findAllById(anySet());
        }
    }

    // ==================== 复杂搜索测试 ====================

    @Nested
    @DisplayName("复杂搜索测试")
    class SearchDemandsTests {

        @Test
        @DisplayName("多条件组合搜索")
        void testSearchDemandsWithMultipleConditions() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Demand> page = new PageImpl<>(List.of(testDemand), pageable, 1);
            when(demandRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Demand> result = demandService.searchDemands(
                TEST_PUBLISHER_ID, "IT技术支持", "PENDING", "浦东",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(10),
                100.0, 1000.0, "测试", "测试用户", pageable
            );

            // Then
            assertNotNull(result);
            verify(demandRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("只按关键词搜索")
        void testSearchDemandsByKeywordOnly() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Demand> page = new PageImpl<>(List.of(testDemand), pageable, 1);
            when(demandRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Demand> result = demandService.searchDemands(
                null, null, null, null, null, null,
                null, null, "测试", null, pageable
            );

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("简化版搜索测试")
        void testSearchDemandsSimple() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Demand> page = new PageImpl<>(List.of(testDemand), pageable, 1);
            when(demandRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Demand> result = demandService.searchDemandsSimple(
                TEST_PUBLISHER_ID, "IT技术支持", "PENDING", "测试", "测试用户", pageable
            );

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("空条件搜索返回所有未删除需求")
        void testSearchDemandsWithNullConditions() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Demand> page = new PageImpl<>(List.of(testDemand), pageable, 1);
            when(demandRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // When
            Page<Demand> result = demandService.searchDemands(
                null, null, null, null, null, null,
                null, null, null, null, pageable
            );

            // Then
            assertNotNull(result);
        }
    }

    // ==================== 边界和异常测试 ====================

    @Nested
    @DisplayName("边界和异常测试")
    class BoundaryAndExceptionTests {

        @Test
        @DisplayName("更新状态时需求不存在")
        void testUpdateStatusDemandNotFound() {
            // Given
            when(demandRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = demandService.updateDemandStatus(999L, "ACCEPTED");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("重置状态时需求不存在")
        void testResetStatusDemandNotFound() {
            // Given
            when(demandRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = demandService.resetStatus(999L, "PENDING");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("发布需求时reward为null也能保存")
        void testPublishDemandWithNullReward() {
            // Given
            Demand demandWithNullReward = new Demand();
            demandWithNullReward.setTitle("无报酬需求");
            demandWithNullReward.setReward(null);
            when(demandRepository.save(any(Demand.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Demand result = demandService.publishDemand(demandWithNullReward);

            // Then
            assertNull(result.getReward());
        }

        @Test
        @DisplayName("发布需求时deadline为null也能保存")
        void testPublishDemandWithNullDeadline() {
            // Given
            Demand demandWithNullDeadline = new Demand();
            demandWithNullDeadline.setTitle("无截止日期需求");
            demandWithNullDeadline.setDeadline(null);
            when(demandRepository.save(any(Demand.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Demand result = demandService.publishDemand(demandWithNullDeadline);

            // Then
            assertNull(result.getDeadline());
        }

        @Test
        @DisplayName("获取发布者昵称时用户仓库异常")
        void testGetPublisherNicknameWithRepositoryException() {
            // Given
            when(userRepository.findById(TEST_PUBLISHER_ID)).thenThrow(new RuntimeException("数据库连接失败"));

            // When & Then
            assertThrows(RuntimeException.class, 
                () -> demandService.getPublisherNickname(TEST_PUBLISHER_ID));
        }
    }
}