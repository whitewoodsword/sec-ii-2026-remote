package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody Map<String, String> params) {
        try {
            User user = userService.register(
                params.get("phone"),
                params.get("password")
            );
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> params) {
        try {
            User user = userService.login(params.get("phone"), params.get("password"));
            Map<String, Object> data = new HashMap<>();
            data.put("token", user.getToken());
            data.put("user", user);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Page<User>>> listUsers(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        Page<User> users = userService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<List<User>>> getRanking(@RequestParam(defaultValue = "10") int limit) {
        List<User> ranking = userService.getScoreRanking(limit);
        return ResponseEntity.ok(ApiResponse.success(ranking));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserService.PlatformStatistics>> getStats() {
        UserService.PlatformStatistics stats = userService.getPlatformStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PutMapping("/name")
    public ResponseEntity<ApiResponse<Void>> editName(@RequestBody Map<String, String> params) {
        try {
            userService.updateUser(Long.parseLong(params.get("id")), params.get("name"), params.get("avatarPath"));
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PutMapping("/avatarPath")
    public ResponseEntity<ApiResponse<Void>> editAvatarPath(@RequestBody Map<String, String> params) {
        try {
            userService.updateUser(Long.parseLong(params.get("id")), params.get("name"), params.get("avatarPath"));
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }
}