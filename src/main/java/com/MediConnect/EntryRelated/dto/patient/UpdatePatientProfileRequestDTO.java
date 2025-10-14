package com.MediConnect.EntryRelated.dto.patient;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class UpdatePatientProfileRequestDTO {
    // Basic Information
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String gender;
    private Date dateOfBirth;
    
    // Physical Information
    private Double height;
    private Double weight;
    private String bloodType;
    
    // Medical Information hi
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
    
    // Insurance Information
    private String insuranceProvider;
    private String insuranceNumber;
    
    // Medications and Lab Results
    private List<CurrentMedicationDTO> medications;
    private List<MentalHealthMedicationDTO> mentalHealthMedications;
    private List<LabResultResponseDTO> labResults;
}
