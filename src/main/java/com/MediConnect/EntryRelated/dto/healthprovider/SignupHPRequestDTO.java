package com.MediConnect.EntryRelated.dto.healthprovider;

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
    private String state;
    private String country;
    private String zipcode;

    private Date educationStartDate;
    private Date educationEndDate;
    private String university;
    private String degree;
    private String licenseNumber;
    private String hospitalName;
    private String clinicName;
    private String department;
    private String bio;
    private Integer yearsOfExperience;
    private Double consultationFee;

    private List<String> availableDays;
    private String availableTimeStart;
    private String availableTimeEnd;

    private List<EducationHistoryDTO> educationHistories;
    private List<WorkExperienceDTO> workExperiences;
}
