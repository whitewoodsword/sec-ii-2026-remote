package com.example.backend.config;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@Order(1) // 确保优先执行
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已有超级管理员
        boolean hasSuperAdmin = userRepository.findAll().stream()
                .anyMatch(User::isSuperAdmin);
        
        System.out.println("========UserInitInfo========");
        if (!hasSuperAdmin) {
            createRootUser();
            System.out.println(" Root super admin user initialized successfully!");
        } else {
            System.out.println("Super admin already exists, skipping initialization.");
        }
        System.out.println("============================");
    }
    
    private void createRootUser() {
        User rootUser = new User();
        rootUser.setName("root");
        rootUser.setPhone("123456");
        rootUser.setPassword(md5("root"));
        rootUser.setScoreNum(0L);
        rootUser.setAverageScore(5.0);
        rootUser.setAdmin(true);
        rootUser.setSuperAdmin(true);
        rootUser.setAvatarPath(null);
        rootUser.setToken(null);
        
        userRepository.save(rootUser);
        
        System.out.println("Root user credentials:");
        System.out.println("  Phone: 123456");
        System.out.println("  Password: root");
        System.out.println("  Role: SUPER_ADMIN");
    }
    
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
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}