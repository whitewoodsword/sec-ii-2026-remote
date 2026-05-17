package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // ========== 用户注册和登录 ==========
    
    /**
     * 用户注册
     */
    @Transactional
    public User register(String phone, String password) {
        // 检查手机号是否已存在
        if (userRepository.existsByPhone(phone)) {
            throw new RuntimeException("手机号已注册");
        }
        
        // 创建新用户
        User user = new User("user"+phone, phone, md5(password));
        user.setScoreNum(0L);
        user.setAverageScore(null);
        user.setAdmin(false);
        user.setSuperAdmin(false);
        
        return userRepository.save(user);
    }
    
    /**
     * 用户登录
     */
    @Transactional
    public User login(String phone, String password) {
        Optional<User> userOpt = userRepository.findByPhone(phone);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        
        User user = userOpt.get();
        if (!user.getPassword().equals(md5(password))) {
            throw new RuntimeException("密码错误");
        }
        
        // 生成token
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setToken(token);
        userRepository.updateToken(user.getId(), token);
        return user;
    }
    
    /**
     * 用户登出
     */
    @Transactional
    public void logout(Long userId) {
        userRepository.clearToken(userId);
    }
    
    /**
     * 根据token获取用户信息
     */
    public User getUserByToken(String token) {
        return userRepository.findByValidToken(token)
                .orElseThrow(() -> new RuntimeException("无效的token"));
    }
    
    // ========== 用户信息管理 ==========
    
    /**
     * 获取用户基本信息
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUser(Long userId, String name, String avatarPath) {
        User user = getUserById(userId);
        
        if (name != null && !name.isEmpty()) {
            user.setName(name);
        }
        if (avatarPath != null) {
            user.setAvatarPath(avatarPath);
        }
        
        return userRepository.save(user);
    }
    
    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        
        if (!user.getPassword().equals(md5(oldPassword))) {
            throw new RuntimeException("原密码错误");
        }
        
        user.setPassword(md5(newPassword));
        userRepository.save(user);
    }
    
    /**
     * 重置密码（管理员操作）
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = getUserById(userId);
        user.setPassword(md5(newPassword));
        userRepository.save(user);
    }
    
    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        // 不能删除超级管理员
        if (user.isSuperAdmin()) {
            throw new RuntimeException("不能删除超级管理员账号");
        }
        userRepository.delete(user);
    }
    
    // ========== 管理员相关操作 ==========
    
    /**
     * 设置为管理员
     */
    @Transactional
    public void setAdmin(Long userId, boolean isAdmin) {
        User user = getUserById(userId);
        // 不能修改超级管理员的权限
        if (user.isSuperAdmin()) {
            throw new RuntimeException("不能修改超级管理员的权限");
        }
        user.setAdmin(isAdmin);
        userRepository.save(user);
    }
    
    /**
     * 获取所有管理员
     */
    public List<User> getAllAdmins() {
        return userRepository.findAllAdmins();
    }
    
    /**
     * 检查是否是管理员
     */
    public boolean isAdmin(Long userId) {
        User user = getUserById(userId);
        return user.isAdmin() || user.isSuperAdmin();
    }
    
    /**
     * 检查是否是超级管理员
     */
    public boolean isSuperAdmin(Long userId) {
        User user = getUserById(userId);
        return user.isSuperAdmin();
    }
    
    // ========== 评分相关功能 ==========
    
    /**
     * 更新用户评分
     * @param userId 被评价的用户ID
     * @param newScore 新评分（1-5）
     */
    @Transactional
    public void updateUserScore(Long userId, int newScore) {
        User user = getUserById(userId);
        
        Long currentScoreNum = user.getScoreNum() != null ? user.getScoreNum() : 0L;
        Double currentAverage = user.getAverageScore() != null ? user.getAverageScore() : 0.0;
        
        // 计算新的平均分
        double totalScore = currentAverage * currentScoreNum + newScore;
        long newScoreNum = currentScoreNum + 1;
        double newAverage = totalScore / newScoreNum;
        
        // 保留两位小数
        newAverage = Math.round(newAverage * 100) / 100.0;
        
        userRepository.updateUserScore(userId, newAverage, newScoreNum);
    }
    
    /**
     * 获取用户评分信息
     */
    public ScoreInfo getUserScoreInfo(Long userId) {
        User user = getUserById(userId);
        ScoreInfo info = new ScoreInfo();
        info.setUserId(user.getId());
        info.setUserName(user.getName());
        info.setAverageScore(user.getAverageScore() != null ? user.getAverageScore() : 0.0);
        info.setScoreNum(user.getScoreNum() != null ? user.getScoreNum() : 0L);
        return info;
    }
    
    /**
     * 获取评分排行榜
     */
    public List<User> getScoreRanking(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return userRepository.findTopNByOrderByAverageScoreDesc(pageable);
    }
    
    // ========== 分页和列表查询 ==========
    
    /**
     * 分页获取所有用户
     */
    public Page<User> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAllByOrderByIdDesc(pageable);
    }
    
    /**
     * 搜索用户
     */
    public List<User> searchUsers(String keyword) {
        return userRepository.findByNameContaining(keyword);
    }
    
    /**
     * 获取所有普通用户（非管理员）
     */
    public List<User> getAllNormalUsers() {
        return userRepository.findAllNormalUsers();
    }
    
    // ========== 统计信息 ==========
    
    /**
     * 获取平台统计信息
     */
    public PlatformStatistics getPlatformStatistics() {
        PlatformStatistics stats = new PlatformStatistics();
        stats.setTotalUsers(userRepository.count());
        stats.setTotalAdmins(userRepository.countAdmins());
        stats.setUsersWithAvatar(userRepository.countUsersWithAvatar());
        stats.setUsersWithHighScore(userRepository.countUsersWithScoreAbove(4.0));
        return stats;
    }
    
    // ========== 辅助类 ==========
    
    /**
     * MD5加密
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }
    
    // ========== 内部类 ==========
    
    /**
     * 评分信息类
     */
    public static class ScoreInfo {
        private Long userId;
        private String userName;
        private Double averageScore;
        private Long scoreNum;
        
        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public Double getAverageScore() { return averageScore; }
        public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }
        public Long getScoreNum() { return scoreNum; }
        public void setScoreNum(Long scoreNum) { this.scoreNum = scoreNum; }
    }
    
    /**
     * 平台统计信息类
     */
    public static class PlatformStatistics {
        private long totalUsers;
        private long totalAdmins;
        private long usersWithAvatar;
        private long usersWithHighScore;
        
        // Getters and Setters
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
        public long getTotalAdmins() { return totalAdmins; }
        public void setTotalAdmins(long totalAdmins) { this.totalAdmins = totalAdmins; }
        public long getUsersWithAvatar() { return usersWithAvatar; }
        public void setUsersWithAvatar(long usersWithAvatar) { this.usersWithAvatar = usersWithAvatar; }
        public long getUsersWithHighScore() { return usersWithHighScore; }
        public void setUsersWithHighScore(long usersWithHighScore) { this.usersWithHighScore = usersWithHighScore; }
    }
}