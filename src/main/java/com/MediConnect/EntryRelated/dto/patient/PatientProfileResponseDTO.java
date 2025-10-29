package com.MediConnect.EntryRelated.dto.patient;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PatientProfileResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;

    private String gender;
    private Date dateOfBirth;
    private String phoneNumber;
    private Date registrationDate;
    
    // Physical Information
    private Double height;
    private Double weight;
    private String bloodType;
    
    // Medical Information
    private String allergies;
    private String medicalConditions;
    private String previousSurgeries;
    private String familyMedicalHistory;
    
    // Lifestyle Information
    private String dietaryHabits;
    private String alcoholConsumption;
    private String physicalActivity;
    private String smokingStatus;
    private String mentalHealthCondition;
    
    // Medications
    private List<MedicationResponseDTO> medications;
    private List<MentalHealthMedicationResponseDTO> mentalHealthMedications;
    
    // Lab Results
    private List<LabResultResponseDTO> labResults;
    
    // Insurance Information
    private String insuranceProvider;
    private String insuranceNumber;
}
