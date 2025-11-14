package com.MediConnect.EntryRelated.entities;

import com.MediConnect.Entities.AppointmentEntity;
import com.MediConnect.EntryRelated.entities.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
public class Patient extends Users {

    @Enumerated(EnumType.STRING)
    private BloodType bloodType;

    private Double height;
    private Double weight;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String medicalConditions;

    @Column(columnDefinition = "TEXT")
    private String previousSurgeries;

    @Column(columnDefinition = "TEXT")
    private String familyMedicalHistory;

    @Enumerated(EnumType.STRING)
    private DietaryHabits dietaryHabits;

    @Enumerated(EnumType.STRING)
    private AlcoholConsumption alcoholConsumption;

    @Enumerated(EnumType.STRING)
    private PhysicalActivity physicalActivity;

    @Enumerated(EnumType.STRING)
    private SmokingStatus smokingStatus;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LaboratoryResult> laboratoryResults;

    @Enumerated(EnumType.STRING)
    private MentalHealthCondition mentalHealthCondition;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MentalHealthMedication> mentalHealthMedications;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Medication> medications;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    private String insuranceProvider;
    private String insuranceNumber;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AppointmentEntity> appointments;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MedicalRecord> medicalRecords;

    private Boolean adminFlagged = false;

    @Column(columnDefinition = "TEXT")
    private String adminFlagReason;

    @Temporal(TemporalType.TIMESTAMP)
    private Date adminFlaggedAt;

    private List<Long> listOfFollowedPeople;
}
