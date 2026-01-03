package com.MediConnect.EntryRelated.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Entity to track profile views for healthcare providers.
 * 
 * Each time a doctor's profile is viewed, a new ProfileView record is created
 * with a timestamp. This allows us to:
 * - Count total profile views
 * - Calculate monthly view statistics
 * - Track view trends over time
 */
@Entity
@Table(name = "profile_views")
@Getter
@Setter
public class ProfileView {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID of the healthcare provider whose profile was viewed
     */
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;
    
    /**
     * Timestamp when the profile was viewed
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "viewed_at", nullable = false)
    private Date viewedAt;
    
    /**
     * Default constructor - sets viewedAt to current time
     */
    public ProfileView() {
        this.viewedAt = new Date();
    }
    
    /**
     * Constructor with doctor ID - automatically sets viewedAt to current time
     */
    public ProfileView(Long doctorId) {
        this.doctorId = doctorId;
        this.viewedAt = new Date();
    }
}

