package com.example.backend.repository;

import com.example.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ========== 基础查询方法 ==========

    // 根据手机号查找用户
    Optional<User> findByPhone(String phone);

    // 根据token查找用户
    Optional<User> findByToken(String token);

    // 检查手机号是否存在
    boolean existsByPhone(String phone);

    // 根据用户ID查找（JPA自带，这里显式声明）
    Optional<User> findById(Long id);

    // ========== 管理员相关查询 ==========

    // 查找超级管理员
    @Query("SELECT u FROM User u WHERE u.isSuperAdmin = true")
    Optional<User> findSuperAdmin();

    // 查找所有超级管理员
    @Query("SELECT u FROM User u WHERE u.isSuperAdmin = true")
    List<User> findAllSuperAdmins();

    // 查找管理员（包括超级管理员）
    @Query("SELECT u FROM User u WHERE u.isAdmin = true OR u.isSuperAdmin = true")
    List<User> findAllAdmins();

    // 检查是否存在超级管理员
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.isSuperAdmin = true")
    boolean existsSuperAdmin();

    // ========== 评分相关查询 ==========

    // 查找评分最高的前N个用户
    List<User> findByOrderByAverageScoreDesc(Pageable pageable);
    // 查找有评分的用户（average_score不为null）
    @Query("SELECT u FROM User u WHERE u.averageScore IS NOT NULL ORDER BY u.averageScore DESC")
    List<User> findUsersWithScores(Pageable pageable);

    // 更新用户平均评分
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.averageScore = :averageScore, u.scoreNum = :scoreNum WHERE u.id = :userId")
    void updateUserScore(@Param("userId") Long userId,
                        @Param("averageScore") Double averageScore,
                        @Param("scoreNum") Long scoreNum);

    // 增加评分次数
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.scoreNum = u.scoreNum + 1 WHERE u.id = :userId")
    void incrementScoreNum(@Param("userId") Long userId);

    // ========== Token相关操作 ==========

    // 更新用户token
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.token = :token WHERE u.id = :userId")
    void updateToken(@Param("userId") Long userId, @Param("token") String token);

    // 清除用户token（登出）
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.token = NULL WHERE u.id = :userId")
    void clearToken(@Param("userId") Long userId);

    // 根据token查找用户并检查是否有效
    @Query("SELECT u FROM User u WHERE u.token = :token AND u.token IS NOT NULL")
    Optional<User> findByValidToken(@Param("token") String token);

    // ========== 分页和列表查询 ==========

    // 分页查询所有用户（按ID倒序）
    Page<User> findAllByOrderByIdDesc(Pageable pageable);

    // 根据姓名模糊查询
    List<User> findByNameContaining(String name);

    // 分页查询管理员
    @Query("SELECT u FROM User u WHERE u.isAdmin = true OR u.isSuperAdmin = true")
    Page<User> findAllAdmins(Pageable pageable);

    // 查询普通用户（非管理员）
    @Query("SELECT u FROM User u WHERE u.isAdmin = false AND u.isSuperAdmin = false")
    List<User> findAllNormalUsers();

    // ========== 批量操作 ==========

    // 批量删除用户（根据ID列表）
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.id IN :ids")
    void deleteAllByIds(@Param("ids") List<Long> ids);

    // 批量设置为管理员
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isAdmin = true WHERE u.id IN :ids")
    void setAdminByIds(@Param("ids") List<Long> ids);

    // 批量取消管理员
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isAdmin = false WHERE u.id IN :ids AND u.isSuperAdmin = false")
    void removeAdminByIds(@Param("ids") List<Long> ids);

    // ========== 统计查询 ==========

    // 统计用户总数
    long count();

    // 统计管理员数量
    @Query("SELECT COUNT(u) FROM User u WHERE u.isAdmin = true OR u.isSuperAdmin = true")
    long countAdmins();

    // 统计有头像的用户数量
    @Query("SELECT COUNT(u) FROM User u WHERE u.avatarPath IS NOT NULL")
    long countUsersWithAvatar();

    // 统计平均评分大于某个值的用户数
    @Query("SELECT COUNT(u) FROM User u WHERE u.averageScore >= :minScore")
    long countUsersWithScoreAbove(@Param("minScore") Double minScore);

    // ========== 复杂查询 ==========

    // 查询用户及其评分信息（用于展示排行榜）
    @Query("SELECT u.id, u.name, u.averageScore, u.scoreNum FROM User u WHERE u.averageScore IS NOT NULL ORDER BY u.averageScore DESC")
    List<Object[]> getUserScoreRanking();

    // 查询最近注册的用户
    List<User> findTop10ByOrderByIdDesc();
}
