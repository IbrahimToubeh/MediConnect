package com.MediConnect.EntryRelated.dto.patient;

import com.MediConnect.EntryRelated.entities.enums.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SignupPatientRequestDTO {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String gender;
    private Date dateOfBirth;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String country;
    private String zipcode;
    //todo: Test test
    private BloodType bloodType;
    private Double height;
    private Double weight;
    private String allergies;
    private String medicalConditions;
    private String previousSurgeries;
    private String familyMedicalHistory;

    private DietaryHabits dietaryHabits;
    private AlcoholConsumption alcoholConsumption;
    private PhysicalActivity physicalActivity;
    private SmokingStatus smokingStatus;

    private MentalHealthCondition mentalHealthCondition;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String insuranceProvider;
    private String insuranceNumber;

    private List<MentalHealthMedicationDTO> medications;
}
