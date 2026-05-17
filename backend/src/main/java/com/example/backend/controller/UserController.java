package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List; 
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> params) {
        User user = userService.register(
            params.get("phone"),
            params.get("password")
        );
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "注册成功");
        result.put("userId", user.getId());
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        User user = userService.login(params.get("phone"), params.get("password"));
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "登录成功");
        result.put("token", user.getToken());
        result.put("user", user);
        return result;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/list")
    public Page<User> listUsers(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size) {
        return userService.getAllUsers(page, size);
    }

    @GetMapping("/ranking")
    public List<User> getRanking(@RequestParam(defaultValue = "10") int limit) {
        return userService.getScoreRanking(limit);
    }

    @GetMapping("/stats")
    public UserService.PlatformStatistics getStats() {
        return userService.getPlatformStatistics();
    }
    @PutMapping("/name")
    public Map<String, Object> editName(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.updateUser(Long.parseLong(params.get("id")), params.get("name"), params.get("avatarPath"));
            result.put("success", true);
            result.put("message", "昵称修改成功");
        } catch (Exception e) {
            
            result.put("success", false);
            result.put("message", "昵称修改失败");
            result.put("error", e.getMessage());
        }
        return result;
    }

    @PutMapping("/avatarPath")
    public Map<String, Object> editAvatarPath(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.updateUser(Long.parseLong(params.get("id")), params.get("name"), params.get("avatarPath"));
            result.put("success", true);
            result.put("message", "用户头像修改成功");
        } catch (Exception e) {
            
            result.put("success", false);
            result.put("message", "用户头像修改失败");
            result.put("error", e.getMessage());
        }
        return result;
    }

    
}