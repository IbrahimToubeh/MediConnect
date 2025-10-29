package com.MediConnect.socialmedia.controller;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.config.JWTService;
import com.MediConnect.socialmedia.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    
    private final NotificationService notificationService;
    
    private final JWTService jwtService;
    
    private final HealthcareProviderRepo healthcareProviderRepo;
    
    private final PatientRepo patientRepo;


    @GetMapping
    public ResponseEntity<?> getUserNotifications(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            System.out.println("=== FETCHING NOTIFICATIONS FOR USER ID: " + userId + " ===");
            
            List<Map<String, Object>> notifications = notificationService.getUserNotifications(userId);
            System.out.println("Found " + notifications.size() + " notifications");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", notifications);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to fetch notifications");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get unread notification count
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            
            Long count = notificationService.getUnreadCount(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching unread count: " + e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to fetch unread count");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Mark notification as read
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long notificationId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            notificationService.markAsRead(notificationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notification marked as read");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error marking notification as read: " + e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to mark notification as read");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Mark all notifications as read
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            
            notificationService.markAllAsRead(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "All notifications marked as read");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error marking all notifications as read: " + e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to mark all notifications as read");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Delete notification
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(
            @PathVariable Long notificationId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            notificationService.deleteNotification(notificationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notification deleted");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error deleting notification: " + e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to delete notification");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Helper method to extract user ID from JWT token
    private Long extractUserIdFromToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // Extract username from JWT token
            String username = jwtService.extractUserName(token);
            
            // Try to find healthcare provider first
            Optional<HealthcareProvider> provider = healthcareProviderRepo.findByUsername(username);
            if (provider.isPresent()) {
                return provider.get().getId();
            }
            
            // If not a healthcare provider, try patient
            Optional<Patient> patient = patientRepo.findByUsername(username);
            if (patient.isPresent()) {
                return patient.get().getId();
            }
            
            throw new RuntimeException("User not found");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract user ID from token: " + e.getMessage());
        }
    }
}

