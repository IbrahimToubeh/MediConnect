package com.MediConnect.controller;

import com.MediConnect.socialmedia.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "POST") String type) {
        
        try {
            String folder = "mediconnect/posts";
            if ("DOCUMENT".equalsIgnoreCase(type)) {
                folder = "mediconnect/documents";
            } else if ("PROFILE".equalsIgnoreCase(type)) {
                folder = "mediconnect/profiles";
            }

            String url = cloudinaryService.uploadFile(file, folder);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("url", url);
            response.put("type", type);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
