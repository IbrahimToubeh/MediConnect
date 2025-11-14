package com.MediConnect.EntryRelated.service.review.impl;

import com.MediConnect.Entities.AppointmentEntity;
import com.MediConnect.Entities.AppointmentStatus;
import com.MediConnect.EntryRelated.entities.DoctorReview;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.repository.AppointmentRepository;
import com.MediConnect.EntryRelated.repository.DoctorReviewRepository;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.service.review.ReviewService;
import com.MediConnect.config.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Review Service Implementation
 * 
 * Handles patient reviews/ratings for completed appointments.
 * Validates that only completed appointments can be reviewed
 * and that patients can only review their own appointments.
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    
    private final DoctorReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepo patientRepo;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final JWTService jwtService;
    
    @Override
    @Transactional
    public Map<String, Object> submitReview(String token, Integer appointmentId, Integer rating, String notes) {
        try {
            // Extract token
            String jwtToken = token != null && token.startsWith("Bearer ") 
                ? token.substring(7) 
                : token;
            
            // Get patient from token
            String username = jwtService.extractUserName(jwtToken);
            Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
            
            // Get appointment
            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
            
            // Validate: Patient can only review their own appointments
            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new RuntimeException("You can only review your own appointments");
            }
            
            // Validate: Only completed appointments can be reviewed
            if (!appointment.getStatus().equals(AppointmentStatus.COMPLETED)) {
                throw new RuntimeException("Only completed appointments can be reviewed");
            }
            
            // Validate: Check if review already exists
            Optional<DoctorReview> existingReview = reviewRepository.findByAppointmentId(appointmentId);
            if (existingReview.isPresent()) {
                throw new RuntimeException("You have already reviewed this appointment");
            }
            
            // Validate rating (1-5)
            if (rating == null || rating < 1 || rating > 5) {
                throw new RuntimeException("Rating must be between 1 and 5");
            }
            
            // Create review
            DoctorReview review = new DoctorReview();
            review.setAppointment(appointment);
            review.setPatient(patient);
            review.setDoctor(appointment.getHealthcareProvider());
            review.setRating(rating);
            review.setNotes(notes != null && !notes.trim().isEmpty() ? notes.trim() : null);
            
            review = reviewRepository.save(review);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Review submitted successfully");
            response.put("reviewId", review.getId());
            response.put("rating", review.getRating());
            return response;
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }
    
    @Override
    public Map<String, Object> getDoctorReviews(String token) {
        try {
            String jwtToken = token != null && token.startsWith("Bearer ") 
                ? token.substring(7) 
                : token;
            
            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider doctor = healthcareProviderRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
            
            List<DoctorReview> reviews = reviewRepository.findByDoctorIdOrderByCreatedAtDesc(doctor.getId());
            
            List<Map<String, Object>> reviewList = new ArrayList<>();
            for (DoctorReview review : reviews) {
                Map<String, Object> reviewMap = new HashMap<>();
                reviewMap.put("id", review.getId());
                reviewMap.put("appointmentId", review.getAppointment().getId());
                reviewMap.put("patientName", review.getPatient().getFirstName() + " " + review.getPatient().getLastName());
                reviewMap.put("rating", review.getRating());
                reviewMap.put("notes", review.getNotes());
                reviewMap.put("createdAt", review.getCreatedAt() != null ? review.getCreatedAt().toInstant().toString() : new Date().toInstant().toString());
                
                // Include appointment date for context
                if (review.getAppointment().getAppointmentDateTime() != null) {
                    reviewMap.put("appointmentDate", review.getAppointment().getAppointmentDateTime().toInstant().toString());
                }
                
                reviewList.add(reviewMap);
            }
            
            // Calculate average rating
            Double avgRating = reviewRepository.findAverageRatingByDoctorId(doctor.getId());
            Long totalReviews = reviewRepository.countByDoctorId(doctor.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", reviewList);
            response.put("averageRating", avgRating != null ? avgRating : 0.0);
            response.put("totalReviews", totalReviews != null ? totalReviews : 0);
            return response;
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }
    
    @Override
    public Map<String, Object> getDoctorRating(Long doctorId) {
        try {
            Double avgRating = reviewRepository.findAverageRatingByDoctorId(doctorId);
            Long totalReviews = reviewRepository.countByDoctorId(doctorId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("averageRating", avgRating != null ? avgRating : 0.0);
            response.put("totalReviews", totalReviews != null ? totalReviews : 0);
            return response;
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }
    
    @Override
    public boolean hasReview(Integer appointmentId) {
        return reviewRepository.findByAppointmentId(appointmentId).isPresent();
    }
}

