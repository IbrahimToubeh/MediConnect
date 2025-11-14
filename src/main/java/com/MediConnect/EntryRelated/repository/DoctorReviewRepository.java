package com.MediConnect.EntryRelated.repository;

import com.MediConnect.EntryRelated.entities.DoctorReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DoctorReview entity
 * Provides methods to query reviews by doctor, patient, or appointment
 */
@Repository
public interface DoctorReviewRepository extends JpaRepository<DoctorReview, Long> {
    
    /**
     * Find all reviews for a specific doctor
     */
    List<DoctorReview> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);
    
    /**
     * Find review for a specific appointment
     */
    Optional<DoctorReview> findByAppointmentId(Integer appointmentId);
    
    /**
     * Find all reviews by a specific patient
     */
    List<DoctorReview> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    
    /**
     * Calculate average rating for a doctor
     */
    @Query("SELECT AVG(r.rating) FROM DoctorReview r WHERE r.doctor.id = :doctorId")
    Double findAverageRatingByDoctorId(@Param("doctorId") Long doctorId);
    
    /**
     * Count total reviews for a doctor
     */
    @Query("SELECT COUNT(r) FROM DoctorReview r WHERE r.doctor.id = :doctorId")
    Long countByDoctorId(@Param("doctorId") Long doctorId);
}

