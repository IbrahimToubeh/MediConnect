package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Review REST Controller
 * 
 * Endpoints for patient reviews/ratings:
 * - POST /reviews/submit - Patient submits a review for a completed appointment
 * - GET /reviews/doctor - Doctor gets all their reviews
 * - GET /reviews/doctor/{doctorId}/rating - Get doctor's average rating (public)
 * - GET /reviews/appointment/{appointmentId}/exists - Check if appointment has review
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    /**
     * Submit a review for a completed appointment
     * Request body: { rating (1-5), notes (optional) }
     */
    @PostMapping("/appointment/{appointmentId}/submit")
    public ResponseEntity<Map<String, Object>> submitReview(
            @PathVariable("appointmentId") Integer appointmentId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Authorization token required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            Integer rating = body.get("rating") != null ? Integer.parseInt(body.get("rating").toString()) : null;
            String notes = body.get("notes") != null ? body.get("notes").toString() : null;
            
            Map<String, Object> response = reviewService.submitReview(authHeader, appointmentId, rating, notes);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get all reviews for the authenticated doctor
     */
    @GetMapping("/doctor")
    public ResponseEntity<Map<String, Object>> getDoctorReviews(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Authorization token required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            Map<String, Object> response = reviewService.getDoctorReviews(authHeader);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get doctor's average rating (public endpoint)
     */
    @GetMapping("/doctor/{doctorId}/rating")
    public ResponseEntity<Map<String, Object>> getDoctorRating(@PathVariable("doctorId") Long doctorId) {
        try {
            Map<String, Object> response = reviewService.getDoctorRating(doctorId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Check if an appointment has a review
     */
    @GetMapping("/appointment/{appointmentId}/exists")
    public ResponseEntity<Map<String, Object>> checkReviewExists(@PathVariable("appointmentId") Integer appointmentId) {
        try {
            boolean exists = reviewService.hasReview(appointmentId);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("hasReview", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

