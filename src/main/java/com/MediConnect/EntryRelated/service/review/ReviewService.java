package com.MediConnect.EntryRelated.service.review;

import java.util.Map;

/**
 * Review Service Interface
 * 
 * Handles patient reviews/ratings for completed appointments.
 * 
 * Features:
 * - Submit review with rating (1-5 stars) and notes
 * - Get all reviews for a doctor
 * - Calculate average rating for a doctor
 * - Get review for a specific appointment
 */
public interface ReviewService {
    
    /**
     * Submit a review for a completed appointment
     * Only patients can submit reviews, and only for their own completed appointments
     * 
     * @param token JWT authentication token
     * @param appointmentId The appointment ID to review
     * @param rating Rating from 1 to 5
     * @param notes Optional feedback notes
     * @return Response with review details
     */
    Map<String, Object> submitReview(String token, Integer appointmentId, Integer rating, String notes);
    
    /**
     * Get all reviews for a doctor
     * Used by doctors to view their feedback page
     * 
     * @param token JWT authentication token
     * @return List of all reviews with patient names and ratings
     */
    Map<String, Object> getDoctorReviews(String token);
    
    /**
     * Get average rating for a doctor
     * Used to display rating on doctor's profile
     * 
     * @param doctorId The doctor's ID
     * @return Average rating (0-5) and total review count
     */
    Map<String, Object> getDoctorRating(Long doctorId);
    
    /**
     * Check if a patient has already reviewed an appointment
     * Used to show/hide review button
     * 
     * @param appointmentId The appointment ID
     * @return True if review exists, false otherwise
     */
    boolean hasReview(Integer appointmentId);
}

