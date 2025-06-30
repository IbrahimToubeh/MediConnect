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
    private String address;//delete
    private String city;//delete
    private String country;//delete
    private Double height;
    private Double weight;
    private String allergies;
    private String medicalConditions;
    private String previousSurgeries;
    private String familyMedicalHistory;

    private String emergencyContactName;// delete
    private String emergencyContactPhone;//delete
    private String emergencyContactRelation;// delete
    private String insuranceProvider;//delete
    private String insuranceNumber;//delete

    private BloodType bloodType;
    private DietaryHabits dietaryHabits;
    private AlcoholConsumption alcoholConsumption;
    private PhysicalActivity physicalActivity;
    private SmokingStatus smokingStatus;
    private MentalHealthCondition mentalHealthCondition;
    private List<CurrentMedicationDTO> medications;
    private List<MentalHealthMedicationDTO> mentalHealthMedications;
}
