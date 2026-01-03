package com.MediConnect.EntryRelated.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "blocked_time_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlockedTimeSlot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private HealthcareProvider provider;
    
    @Temporal(TemporalType.DATE)
    @Column(name = "blocked_date", nullable = false)
    private Date blockedDate;
    
    @Column(name = "start_time", length = 5, nullable = false)
    private String startTime; // HH:mm format
    
    @Column(name = "end_time", length = 5, nullable = false)
    private String endTime; // HH:mm format
    
    @Column(columnDefinition = "TEXT")
    private String reason; // Optional reason for blocking (e.g., "Phone call appointment")
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}

