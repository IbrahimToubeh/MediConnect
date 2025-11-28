package com.MediConnect.EntryRelated.dto.healthprovider;

import com.MediConnect.EntryRelated.entities.SpecializationType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SignupHPRequestDTO {
    @NotBlank(message = "Username is required")
    @Size(min = 3, message = "Username must be at least 3 characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Gender is required")
    private String gender;
    
    @NotNull(message = "Date of birth is required")
    private String dateOfBirth;
    
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    private String address;
    private String city;
    private String country;

    private Double consultationFee;
    private String bio;
    private String clinicName;
    private String licenseNumber;
    private String licenseDocumentUrl;
    private List<String> availableDays;
    private String availableTimeStart;
    private String availableTimeEnd;
    private List<String> insuranceAccepted;
    private String profilePicture;
    private String bannerPicture;

    private List<String> specializations;
    private List<EducationHistoryDTO> educationHistories;
    private List<WorkExperienceDTO> workExperiences;
}
