package com.MediConnect.EntryRelated.dto.patient;

import com.MediConnect.EntryRelated.entities.enums.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class
SignupPatientRequestDTO {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String gender;
    private Date dateOfBirth;
    private String phoneNumber;
    private Double height;
    private Double weight;
    private String allergies;
    private String medicalConditions;
    private String previousSurgeries;
    private String familyMedicalHistory;


    private BloodType bloodType;
    private DietaryHabits dietaryHabits;
    private AlcoholConsumption alcoholConsumption;
    private PhysicalActivity physicalActivity;
    private SmokingStatus smokingStatus;
    private MentalHealthCondition mentalHealthCondition;
    private List<CurrentMedicationDTO> medications;
    private List<MentalHealthMedicationDTO> mentalHealthMedications;
    private List<LaboratoryResultDTO> laboratoryResults;
    
    // Insurance Information
    private String insuranceProvider;
    private String insuranceNumber;
}
