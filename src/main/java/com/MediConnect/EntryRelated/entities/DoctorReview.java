package com.MediConnect.EntryRelated.entities;

import com.MediConnect.Entities.AppointmentEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Review Entity
 * 
 * Stores patient reviews/ratings for completed appointments.
 * Each review is linked to a specific appointment and doctor.
 * 
 * Features:
 * - 1-5 star rating
 * - Optional notes/feedback
 * - Links to appointment, patient, and doctor
 * - Timestamp for when review was created
 */
@Entity
@Table(name = "doctor_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoctorReview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The appointment this review is for
     * Only completed appointments can be reviewed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private AppointmentEntity appointment;
    
    /**
     * The patient who wrote the review
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    /**
     * The doctor being reviewed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private HealthcareProvider doctor;
    
    /**
     * Rating from 1 to 5 stars
     */
    @Column(nullable = false)
    private Integer rating; // 1-5
    
    /**
     * Optional feedback notes from the patient
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    /**
     * When the review was created
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}

