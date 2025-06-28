package com.MediConnect.EntryRelated.dto.healthprovider;

import com.MediConnect.EntryRelated.entities.SpecializationType;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SignupHPRequestDTO {
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
    private String country;

    private Double consultationFee;
    private String bio;
    private String clinicName;
    private String licenseNumber;
    private List<String> availableDays;
    private String availableTimeStart;
    private String availableTimeEnd;

    private List<SpecializationType> specializations;
    private List<EducationHistoryDTO> educationHistories;
    private List<WorkExperienceDTO> workExperiences;
}
