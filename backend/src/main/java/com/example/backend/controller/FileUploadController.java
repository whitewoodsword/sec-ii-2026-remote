package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadFiles(
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "请选择要上传的文件"));
            }

            List<String> fileUrls = fileUploadService.uploadFiles(files);
            
            Map<String, Object> result = new HashMap<>();
            result.put("urls", fileUrls);
            result.put("count", fileUrls.size());

            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (IOException e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "文件上传失败: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-single")
    public ResponseEntity<ApiResponse<String>> uploadSingleFile(
            @RequestParam("file") MultipartFile file) {
        
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "请选择要上传的文件"));
            }

            String fileUrl = fileUploadService.uploadSingleFile(file);
            
            if (fileUrl == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "文件上传失败"));
            }

            return ResponseEntity.ok(ApiResponse.success(fileUrl));
            
        } catch (IOException e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "文件上传失败: " + e.getMessage()));
        }
    }
}