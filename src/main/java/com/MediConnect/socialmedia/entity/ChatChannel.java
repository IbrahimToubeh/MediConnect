package com.MediConnect.socialmedia.entity;

import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.Entities.AppointmentEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Chat Channel Entity
 * 
 * Represents a chat conversation between a patient and a doctor.
 * 
 * Business Rules:
 * - Only ONE chat channel exists per patient-doctor pair
 * - Chat channel is automatically created when an appointment is confirmed
 * - Channel remains active even after appointments are completed
 * - Both patient and doctor can access this channel
 */
@Entity
@Table(name = "chat_channels", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "doctor_id"}))
@Getter
@Setter
public class ChatChannel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Patient in this chat channel
     * Required - every chat must have a patient
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    /**
     * Doctor in this chat channel
     * Required - every chat must have a doctor
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private HealthcareProvider doctor;
    
    /**
     * The appointment that triggered this chat channel creation
     * Required - every chat channel must be associated with a confirmed appointment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private AppointmentEntity appointment;
    
    /**
     * When this chat channel was created
     * Automatically set when channel is first created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    /**
     * Last activity timestamp
     * Updated whenever a message is sent in this channel
     * Used to sort channels by most recent activity
     */
    @Column(name = "last_activity_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActivityAt;
    
    /**
     * Whether this channel is currently active
     * Can be used to archive old conversations in the future
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /**
     * Set default timestamps before saving
     */
    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastActivityAt == null) {
            lastActivityAt = now;
        }
    }
}

