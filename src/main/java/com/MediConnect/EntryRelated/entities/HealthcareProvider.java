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

    @OneToMany(mappedBy = "healthcareProvider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AppointmentEntity> appointments;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MedicalRecord> medicalRecords;
}