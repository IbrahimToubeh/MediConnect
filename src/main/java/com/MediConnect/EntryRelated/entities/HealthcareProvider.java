package com.MediConnect.EntryRelated.entities;

import com.MediConnect.Entities.AppointmentEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
public class HealthcareProvider extends Users {

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Column(name = "specialization")
    private List<SpecializationType> specializations;

    private String licenseNumber;
    private String licenseDocumentUrl;
    
    @Column(columnDefinition = "BYTEA")
    private byte[] licenseDocument;

    private String licenseDocumentContentType;

    private String clinicName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private Double consultationFee;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EducationHistory> educationHistories;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkExperience> workExperiences;

    @ElementCollection
    @CollectionTable(name = "provider_availability")
    private List<String> availableDays;

    private String availableTimeStart;
    private String availableTimeEnd;

    @ElementCollection
    @CollectionTable(name = "provider_insurance")
    private List<String> insuranceAccepted;

    @OneToMany(mappedBy = "healthcareProvider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AppointmentEntity> appointments;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MedicalRecord> medicalRecords;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DayAvailability> dayAvailabilities;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BlockedTimeSlot> blockedTimeSlots;

    @Column(name = "appointment_duration_minutes")
    private Integer appointmentDurationMinutes = 30; // Default 30 minutes

    private Boolean adminFlagged = false;

    @Column(columnDefinition = "TEXT")
    private String adminFlagReason;

    @Temporal(TemporalType.TIMESTAMP)
    private Date adminFlaggedAt;

    private List<Long> followList;
}