package com.MediConnect.EntryRelated.controller;

import com.MediConnect.config.JWTService;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.service.analytics.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for doctor analytics
 * 
 * Provides endpoint to retrieve comprehensive analytics data for doctors
 * including profile views, post interactions, appointment statistics, and more.
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    private final JWTService jwtService;
    private final HealthcareProviderRepo healthcareProviderRepo;
    
    /**
     * Get analytics for the authenticated doctor
     * 
     * Returns:
     * - Profile views (total, this month, change percentage)
     * - Post statistics (total posts, likes, comments, engagement rate)
     * - Appointment statistics (total, by status, monthly trends)
     * - Patient growth (total patients, new patients this month)
     * - Engagement trends (monthly post engagement)
     * - Key metrics (conversion rate, completion rate)
     * 
     * @param request HTTP request containing JWT token
     * @return ResponseEntity with analytics data
     */
    @GetMapping("/doctor")
    public ResponseEntity<Map<String, Object>> getDoctorAnalytics(HttpServletRequest request) {
        try {
            // Extract token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse("Authorization token required"));
            }
            
            String token = authHeader.substring(7);
            String username = jwtService.extractUserName(token);
            
            // Find doctor by username
            Long doctorId = healthcareProviderRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"))
                .getId();
            
            // Get analytics data
            Map<String, Object> analytics = analyticsService.getDoctorAnalytics(doctorId);
            
            return ResponseEntity.ok(successResponse("data", analytics));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Helper method to create success response
     */
    private Map<String, Object> successResponse(String key, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put(key, data);
        return response;
    }
    
    /**
     * Helper method to create error response
     */
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }
}

