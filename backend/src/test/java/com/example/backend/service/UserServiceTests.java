package com.example.backend.service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final String TEST_PHONE = "13800138000";
    private final String TEST_PASSWORD = "123456";
    private final String TEST_MD5_PASSWORD = "e10adc3949ba59abbe56e057f20f883e"; // md5 of 123456
    private final Long TEST_USER_ID = 1L;
    private final String TEST_TOKEN = "test_token_12345";

    @BeforeEach
    void setUp() {
        testUser = new User("测试用户", TEST_PHONE, TEST_MD5_PASSWORD);
        testUser.setId(TEST_USER_ID);
        testUser.setScoreNum(0L);
        testUser.setAverageScore(null);
        testUser.setAdmin(false);
        testUser.setSuperAdmin(false);
        testUser.setToken(null);
    }

    // ==================== 用户注册和登录测试 ====================

    @Nested
    @DisplayName("用户注册测试")
    class RegisterTests {

        @Test
        @DisplayName("成功注册新用户")
        void testRegisterSuccess() {
            // Given
            when(userRepository.existsByPhone(TEST_PHONE)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(TEST_USER_ID);
                return savedUser;
            });

            // When
            User registered = userService.register(TEST_PHONE, TEST_PASSWORD);

            // Then
            assertNotNull(registered);
            assertEquals(TEST_PHONE, registered.getPhone());
            assertEquals("user" + TEST_PHONE, registered.getName());
            assertEquals(TEST_MD5_PASSWORD, registered.getPassword());
            assertEquals(0L, registered.getScoreNum());
            assertNull(registered.getAverageScore());
            assertFalse(registered.isAdmin());
            assertFalse(registered.isSuperAdmin());

            verify(userRepository).existsByPhone(TEST_PHONE);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("注册时手机号已存在应抛出异常")
        void testRegisterPhoneAlreadyExists() {
            // Given
            when(userRepository.existsByPhone(TEST_PHONE)).thenReturn(true);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.register(TEST_PHONE, TEST_PASSWORD));
            assertEquals("手机号已注册", exception.getMessage());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("注册时手机号为空应抛出异常")
        void testRegisterWithEmptyPhone() {
            // When & Then
            assertThrows(Exception.class, 
                () -> userService.register("", TEST_PASSWORD));
        }
    }

    @Nested
    @DisplayName("用户登录测试")
    class LoginTests {

        @Test
        @DisplayName("成功登录并生成token")
        void testLoginSuccess() {
            // Given
            testUser.setPassword(TEST_MD5_PASSWORD);
            when(userRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(testUser));
            doNothing().when(userRepository).updateToken(eq(TEST_USER_ID), anyString());

            // When
            User loggedIn = userService.login(TEST_PHONE, TEST_PASSWORD);

            // Then
            assertNotNull(loggedIn);
            assertNotNull(loggedIn.getToken());
            assertEquals(TEST_USER_ID, loggedIn.getId());

            verify(userRepository).findByPhone(TEST_PHONE);
            verify(userRepository).updateToken(eq(TEST_USER_ID), anyString());
        }

        @Test
        @DisplayName("登录时用户不存在应抛出异常")
        void testLoginUserNotFound() {
            // Given
            when(userRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.login(TEST_PHONE, TEST_PASSWORD));
            assertEquals("用户不存在", exception.getMessage());

            verify(userRepository, never()).updateToken(any(), any());
        }

        @Test
        @DisplayName("登录时密码错误应抛出异常")
        void testLoginWrongPassword() {
            // Given
            testUser.setPassword("wrong_md5_password");
            when(userRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(testUser));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.login(TEST_PHONE, TEST_PASSWORD));
            assertEquals("密码错误", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Token管理测试")
    class TokenTests {

        @Test
        @DisplayName("根据有效token获取用户")
        void testGetUserByValidToken() {
            // Given
            testUser.setToken(TEST_TOKEN);
            when(userRepository.findByValidToken(TEST_TOKEN)).thenReturn(Optional.of(testUser));

            // When
            User user = userService.getUserByToken(TEST_TOKEN);

            // Then
            assertNotNull(user);
            assertEquals(TEST_USER_ID, user.getId());
            assertEquals(TEST_TOKEN, user.getToken());
        }

        @Test
        @DisplayName("根据无效token获取用户应抛出异常")
        void testGetUserByInvalidToken() {
            // Given
            when(userRepository.findByValidToken("invalid_token")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.getUserByToken("invalid_token"));
            assertEquals("无效的token", exception.getMessage());
        }

        @Test
        @DisplayName("用户登出应清除token")
        void testLogout() {
            // Given
            doNothing().when(userRepository).clearToken(TEST_USER_ID);

            // When
            userService.logout(TEST_USER_ID);

            // Then
            verify(userRepository).clearToken(TEST_USER_ID);
        }
    }

    // ==================== 用户信息管理测试 ====================

    @Nested
    @DisplayName("用户信息管理测试")
    class UserInfoManagementTests {

        @Test
        @DisplayName("成功获取用户信息")
        void testGetUserByIdSuccess() {
            // Given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

            // When
            User found = userService.getUserById(TEST_USER_ID);

            // Then
            assertNotNull(found);
            assertEquals(TEST_USER_ID, found.getId());
            assertEquals("测试用户", found.getName());
        }

        @Test
        @DisplayName("获取不存在的用户应抛出异常")
        void testGetUserByIdNotFound() {
            // Given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.getUserById(TEST_USER_ID));
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("成功更新用户名和头像")
        void testUpdateUserSuccess() {
            // Given
            String newName = "新名字";
            String newAvatar = "/avatars/avatar.png";
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User updated = userService.updateUser(TEST_USER_ID, newName, newAvatar);

            // Then
            assertEquals(newName, updated.getName());
            assertEquals(newAvatar, updated.getAvatarPath());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("只更新用户名不更新头像")
        void testUpdateUserOnlyName() {
            // Given
            String newName = "新名字";
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User updated = userService.updateUser(TEST_USER_ID, newName, null);

            // Then
            assertEquals(newName, updated.getName());
            assertNull(updated.getAvatarPath());
        }

        @Test
        @DisplayName("成功修改密码")
        void testChangePasswordSuccess() {
            // Given
            String newPassword = "654321";
            String newMd5Password = "c33367701511b4f6020ec61ded352059"; // md5 of 654321
            testUser.setPassword(TEST_MD5_PASSWORD);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.changePassword(TEST_USER_ID, TEST_PASSWORD, newPassword);

            // Then
            assertEquals(newMd5Password, testUser.getPassword());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("修改密码时原密码错误应抛出异常")
        void testChangePasswordWrongOldPassword() {
            // Given
            testUser.setPassword(TEST_MD5_PASSWORD);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.changePassword(TEST_USER_ID, "wrong_password", "654321"));
            assertEquals("原密码错误", exception.getMessage());
        }


        @Test
        @DisplayName("删除普通用户成功")
        void testDeleteNormalUserSuccess() {
            // Given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(userRepository).delete(testUser);

            // When
            userService.deleteUser(TEST_USER_ID);

            // Then
            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("删除超级管理员应抛出异常")
        void testDeleteSuperAdmin() {
            // Given
            testUser.setSuperAdmin(true);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.deleteUser(TEST_USER_ID));
            assertEquals("不能删除超级管理员账号", exception.getMessage());
            verify(userRepository, never()).delete(any());
        }
    }

    // ==================== 管理员相关操作测试 ====================

    @Nested
    @DisplayName("管理员操作测试")
    class AdminOperationTests {

        @Test
        @DisplayName("将普通用户设置为管理员")
        void testSetUserAsAdmin() {
            // Given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.setAdmin(TEST_USER_ID, true);

            // Then
            assertTrue(testUser.isAdmin());
            assertFalse(testUser.isSuperAdmin());
        }

        @Test
        @DisplayName("取消用户的管理员权限")
        void testRemoveAdmin() {
            // Given
            testUser.setAdmin(true);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.setAdmin(TEST_USER_ID, false);

            // Then
            assertFalse(testUser.isAdmin());
        }

        @Test
        @DisplayName("不能修改超级管理员的权限")
        void testCannotModifySuperAdmin() {
            // Given
            testUser.setSuperAdmin(true);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.setAdmin(TEST_USER_ID, true));
            assertEquals("不能修改超级管理员的权限", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("获取所有管理员列表")
        void testGetAllAdmins() {
            // Given
            List<User> admins = List.of(testUser);
            when(userRepository.findAllAdmins()).thenReturn(admins);

            // When
            List<User> result = userService.getAllAdmins();

            // Then
            assertEquals(1, result.size());
            verify(userRepository).findAllAdmins();
        }

        @Test
        @DisplayName("检查用户是否是管理员")
        void testIsAdmin() {
            // Given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

            // When - 普通用户
            boolean isAdmin = userService.isAdmin(TEST_USER_ID);
            assertFalse(isAdmin);

            // When - 设置为管理员后
            testUser.setAdmin(true);
            boolean isAdminNow = userService.isAdmin(TEST_USER_ID);
            assertTrue(isAdminNow);

            // When - 超级管理员
            testUser.setSuperAdmin(true);
            boolean isSuperAdmin = userService.isAdmin(TEST_USER_ID);
            assertTrue(isSuperAdmin);
        }
    }

    // ==================== 评分相关功能测试 ====================

    @Nested
    @DisplayName("评分功能测试")
    class ScoreTests {

        @Test
        @DisplayName("首次给用户评分")
        void testFirstTimeScore() {
            // Given
            int newScore = 5;
            testUser.setScoreNum(0L);
            testUser.setAverageScore(null);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(userRepository).updateUserScore(eq(TEST_USER_ID), anyDouble(), eq(1L));

            // When
            userService.updateUserScore(TEST_USER_ID, newScore);

            // Then
            verify(userRepository).updateUserScore(eq(TEST_USER_ID), eq(5.0), eq(1L));
        }

        @Test
        @DisplayName("多次评分计算平均分")
        void testMultipleScores() {
            // Given
            testUser.setScoreNum(2L);
            testUser.setAverageScore(4.0); // 已有平均分4.0
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(userRepository).updateUserScore(eq(TEST_USER_ID), anyDouble(), eq(3L));

            // When - 给3分
            userService.updateUserScore(TEST_USER_ID, 3);

            // Then - 新平均分 = (4*2 + 3)/3 = 11/3 ≈ 3.67
            verify(userRepository).updateUserScore(eq(TEST_USER_ID), eq(3.67), eq(3L));
        }

        @Test
        @DisplayName("获取用户评分信息")
        void testGetUserScoreInfo() {
            // Given
            testUser.setScoreNum(10L);
            testUser.setAverageScore(4.5);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

            // When
            UserService.ScoreInfo scoreInfo = userService.getUserScoreInfo(TEST_USER_ID);

            // Then
            assertNotNull(scoreInfo);
            assertEquals(TEST_USER_ID, scoreInfo.getUserId());
            assertEquals("测试用户", scoreInfo.getUserName());
            assertEquals(4.5, scoreInfo.getAverageScore());
            assertEquals(10L, scoreInfo.getScoreNum());
        }

        @Test
        @DisplayName("获取评分排行榜")
        void testGetScoreRanking() {
            // Given
            List<User> ranking = List.of(testUser);
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findTopNByOrderByAverageScoreDesc(pageable)).thenReturn(ranking);

            // When
            List<User> result = userService.getScoreRanking(10);

            // Then
            assertEquals(1, result.size());
            verify(userRepository).findTopNByOrderByAverageScoreDesc(pageable);
        }
    }

    // ==================== 分页和列表查询测试 ====================

    @Nested
    @DisplayName("分页和列表查询测试")
    class PaginationTests {

        @Test
        @DisplayName("分页获取所有用户")
        void testGetAllUsers() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(testUser), pageable, 1);
            when(userRepository.findAllByOrderByIdDesc(pageable)).thenReturn(page);

            // When
            Page<User> result = userService.getAllUsers(0, 10);

            // Then
            assertEquals(1, result.getTotalElements());
            assertEquals(testUser.getId(), result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("搜索用户")
        void testSearchUsers() {
            // Given
            String keyword = "测试";
            List<User> users = List.of(testUser);
            when(userRepository.findByNameContaining(keyword)).thenReturn(users);

            // When
            List<User> result = userService.searchUsers(keyword);

            // Then
            assertEquals(1, result.size());
            verify(userRepository).findByNameContaining(keyword);
        }

        @Test
        @DisplayName("获取所有普通用户")
        void testGetAllNormalUsers() {
            // Given
            List<User> normalUsers = List.of(testUser);
            when(userRepository.findAllNormalUsers()).thenReturn(normalUsers);

            // When
            List<User> result = userService.getAllNormalUsers();

            // Then
            assertEquals(1, result.size());
            verify(userRepository).findAllNormalUsers();
        }
    }

    // ==================== 统计信息测试 ====================

    @Nested
    @DisplayName("统计信息测试")
    class StatisticsTests {

        @Test
        @DisplayName("获取平台统计信息")
        void testGetPlatformStatistics() {
            // Given
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countAdmins()).thenReturn(5L);
            when(userRepository.countUsersWithAvatar()).thenReturn(30L);
            when(userRepository.countUsersWithScoreAbove(4.0)).thenReturn(20L);

            // When
            UserService.PlatformStatistics stats = userService.getPlatformStatistics();

            // Then
            assertEquals(100L, stats.getTotalUsers());
            assertEquals(5L, stats.getTotalAdmins());
            assertEquals(30L, stats.getUsersWithAvatar());
            assertEquals(20L, stats.getUsersWithHighScore());
        }
    }

    // ==================== MD5加密测试 ====================

    @Nested
    @DisplayName("MD5加密测试")
    class Md5Tests {

        @Test
        @DisplayName("MD5加密方法应正确加密字符串")
        void testMd5Encryption() throws Exception {
            // 使用反射访问私有方法
            Method md5Method = UserService.class.getDeclaredMethod("md5", String.class);
            md5Method.setAccessible(true);

            String encrypted = (String) md5Method.invoke(userService, "123456");
            assertEquals("e10adc3949ba59abbe56e057f20f883e", encrypted);
            
            String emptyEncrypted = (String) md5Method.invoke(userService, "");
            assertEquals("d41d8cd98f00b204e9800998ecf8427e", emptyEncrypted);
        }
    }

    // ==================== 边界条件和异常测试 ====================

    @Nested
    @DisplayName("边界条件和异常测试")
    class BoundaryAndExceptionTests {

        @Test
        @DisplayName("更新不存在的用户应抛出异常")
        void testUpdateNonExistentUser() {
            // Given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, 
                () -> userService.updateUser(TEST_USER_ID, "新名字", null));
        }

        @Test
        @DisplayName("更新评分时用户不存在应抛出异常")
        void testUpdateScoreForNonExistentUser() {
            // Given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, 
                () -> userService.updateUserScore(TEST_USER_ID, 5));
        }

        @Test
        @DisplayName("重置密码时用户不存在应抛出异常")
        void testResetPasswordForNonExistentUser() {
            // Given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, 
                () -> userService.resetPassword(TEST_USER_ID, "newpass"));
        }

        @Test
        @DisplayName("更新用户信息时传入空值应保持不变")
        void testUpdateUserWithNullValues() {
            // Given
            String originalName = testUser.getName();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            User updated = userService.updateUser(TEST_USER_ID, null, null);

            // Then
            assertEquals(originalName, updated.getName());
            assertNull(updated.getAvatarPath());
        }

        @Test
        @DisplayName("评分排行榜限制数量测试")
        void testScoreRankingLimit() {
            // Given
            int limit = 5;
            Pageable pageable = PageRequest.of(0, limit);
            when(userRepository.findTopNByOrderByAverageScoreDesc(pageable)).thenReturn(List.of());

            // When
            userService.getScoreRanking(limit);

            // Then
            verify(userRepository).findTopNByOrderByAverageScoreDesc(pageable);
        }
    }
}