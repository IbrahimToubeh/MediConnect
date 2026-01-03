package com.MediConnect.EntryRelated.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "day_availability",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"provider_id", "day_of_week"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DayAvailability {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private HealthcareProvider provider;
    
    @Column(nullable = false, length = 20, name = "day_of_week")
    private String dayOfWeek; // "Monday", "Tuesday", etc.
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "start_time", length = 5)
    private String startTime; // HH:mm format
    
    @Column(name = "end_time", length = 5)
    private String endTime; // HH:mm format
}

