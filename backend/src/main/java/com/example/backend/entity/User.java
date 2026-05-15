package com.example.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone", nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "score_num")
    private Long scoreNum;

    @Column(name = "token")
    private String token;

    @Column(name = "average_score")
    private Double averageScore;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "is_admin")
    private boolean isAdmin;

    @Column(name = "is_super_admin")
    private boolean isSuperAdmin;

    public User() {}

    public User(String name, String phone, String password) {
        this.name = name;
        this.phone = phone;
        this.password = password;
        this.scoreNum = 0L;
        this.isAdmin = false;
        this.isSuperAdmin = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Long getScoreNum() { return scoreNum; }
    public void setScoreNum(Long scoreNum) { this.scoreNum = scoreNum; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Double getAverageScore() { return averageScore; }
    public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public boolean isSuperAdmin() { return isSuperAdmin; }
    public void setSuperAdmin(boolean superAdmin) { isSuperAdmin = superAdmin; }
}