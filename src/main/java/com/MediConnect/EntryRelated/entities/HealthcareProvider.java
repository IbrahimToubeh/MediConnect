package com.MediConnect.EntryRelated.entities;

import com.MediConnect.Entities.AppointmentEntity;
import com.MediConnect.Entities.Specialization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
public class HealthcareProvider extends Users {

    @Temporal(TemporalType.DATE)
    private Date educationStartDate;

    @Temporal(TemporalType.DATE)
    private Date educationEndDate;

    private String university;
    private String degree;

    @ManyToMany
    @JoinTable(
        name = "provider_specializations",
        joinColumns = @JoinColumn(name = "provider_id"),
        inverseJoinColumns = @JoinColumn(name = "specialization_id")
    )
    private List<Specialization> specializations;
    private String licenseNumber;
    private String hospitalName;
    private String clinicName;
    private String department;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private Integer yearsOfExperience;
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