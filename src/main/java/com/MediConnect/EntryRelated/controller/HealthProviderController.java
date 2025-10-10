package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.healthprovider.GetAllSpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.LoginHPRequestDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import com.MediConnect.EntryRelated.entities.SpecializationType;
import com.MediConnect.EntryRelated.service.healthprovider.HealthcareProviderService;
import com.MediConnect.Service.UserService;
import com.MediConnect.config.JWTService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/healthprovider")
public class HealthProviderController {

    private final HealthcareProviderService healthcareProviderService;
    private final UserService userService;
    private final JWTService jwtService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginHPRequestDTO healthProviderInfo) {
        try {
            System.out.println("DEBUG LOGIN: Attempting login for username: '" + healthProviderInfo.getUsername() + "'");
            
            // First verify the user is a healthcare provider before authentication
            HealthcareProvider provider = healthcareProviderService.findByUsername(healthProviderInfo.getUsername())
                    .orElseThrow(() -> {
                        System.out.println("ERROR LOGIN: Healthcare provider not found for username: '" + healthProviderInfo.getUsername() + "'");
                        return new RuntimeException("Healthcare provider not found");
                    });
            
            System.out.println("DEBUG LOGIN: Found provider with role: '" + provider.getRole() + "'");
            
            // Verify the user has HEALTHPROVIDER role
            if (!"HEALTHPROVIDER".equals(provider.getRole())) {
                System.out.println("ERROR LOGIN: Wrong role. Expected 'HEALTHPROVIDER', got: '" + provider.getRole() + "'");
                throw new RuntimeException("Access denied: This account is not a healthcare provider account");
            }
            
            System.out.println("DEBUG LOGIN: Attempting authentication with Spring Security");
            // Then authenticate the user
            String token = userService.authenticate(healthProviderInfo.getUsername(), healthProviderInfo.getPassword());
            
            System.out.println("DEBUG LOGIN: Authentication successful, token generated");
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Healthcare provider login successful");
            response.put("token", token);
            response.put("userId", provider.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERROR LOGIN: Exception during login: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody SignupHPRequestDTO healthProviderInfo) {
        try {
            String result = healthcareProviderService.register(healthProviderInfo);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("GetAllSpecialty")
    public List<GetAllSpecialtyDTO> GetAllSpecialty() {
        return healthcareProviderService.getAllSpecialtyDTO();
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestHeader("Authorization") String token) {
        try {
            // Extract username from JWT token
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            
            // Find healthcare provider by username
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));
            
            // Build response with provider data
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", buildHealthcareProviderProfileResponseDTO(provider));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody SignupHPRequestDTO updateRequest) {
        try {
            // Extract username from JWT token
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            
            // Find healthcare provider by username
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));
            
            // Update provider fields
            updateHealthcareProviderFields(provider, updateRequest);
            
            // Save updated provider
            HealthcareProvider savedProvider = healthcareProviderService.save(provider);
            
            // Build response with updated provider data
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Profile updated successfully");
            response.put("data", buildHealthcareProviderProfileResponseDTO(savedProvider));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    private Map<String, Object> buildHealthcareProviderProfileResponseDTO(HealthcareProvider provider) {
        Map<String, Object> profile = new HashMap<>();
        
        // Basic Information
        profile.put("id", provider.getId());
        profile.put("username", provider.getUsername());
        profile.put("email", provider.getEmail());
        profile.put("firstName", provider.getFirstName());
        profile.put("lastName", provider.getLastName());
        profile.put("gender", provider.getGender());
        profile.put("dateOfBirth", provider.getDateOfBirth());
        profile.put("phoneNumber", provider.getPhoneNumber());
        profile.put("address", provider.getAddress());
        profile.put("city", provider.getCity());
        profile.put("country", provider.getCountry());
        
        // Professional Information
        profile.put("consultationFee", provider.getConsultationFee());
        profile.put("bio", provider.getBio());
        profile.put("clinicName", provider.getClinicName());
        profile.put("licenseNumber", provider.getLicenseNumber());
        profile.put("availableDays", provider.getAvailableDays());
        profile.put("availableTimeStart", provider.getAvailableTimeStart());
        profile.put("availableTimeEnd", provider.getAvailableTimeEnd());
        
        // Specializations
        if (provider.getSpecializations() != null) {
            profile.put("specializations", provider.getSpecializations().stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toList()));
        } else {
            profile.put("specializations", new java.util.ArrayList<>());
        }
        
        // Education Histories
        if (provider.getEducationHistories() != null) {
            profile.put("educationHistories", provider.getEducationHistories().stream()
                .map(this::convertToEducationHistoryDTO)
                .collect(java.util.stream.Collectors.toList()));
        } else {
            profile.put("educationHistories", new java.util.ArrayList<>());
        }
        
        // Work Experiences
        if (provider.getWorkExperiences() != null) {
            profile.put("workExperiences", provider.getWorkExperiences().stream()
                .map(this::convertToWorkExperienceDTO)
                .collect(java.util.stream.Collectors.toList()));
        } else {
            profile.put("workExperiences", new java.util.ArrayList<>());
        }
        
        return profile;
    }

    private Map<String, Object> convertToEducationHistoryDTO(EducationHistory education) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", education.getId());
        dto.put("institutionName", education.getInstitutionName());
        dto.put("startDate", education.getStartDate());
        dto.put("endDate", education.getEndDate());
        dto.put("stillEnrolled", education.isStillEnrolled());
        return dto;
    }

    private Map<String, Object> convertToWorkExperienceDTO(WorkExperience work) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", work.getId());
        dto.put("organizationName", work.getOrganizationName());
        dto.put("roleTitle", work.getRoleTitle());
        dto.put("startDate", work.getStartDate());
        dto.put("endDate", work.getEndDate());
        dto.put("stillWorking", work.isStillWorking());
        return dto;
    }

    private void updateHealthcareProviderFields(HealthcareProvider provider, SignupHPRequestDTO updateRequest) {
        System.out.println("DEBUG: Updating healthcare provider fields");
        // Basic Information
        if (updateRequest.getFirstName() != null && !updateRequest.getFirstName().isEmpty()) {
            provider.setFirstName(updateRequest.getFirstName());
            System.out.println("DEBUG: Updated firstName: " + updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null && !updateRequest.getLastName().isEmpty()) {
            provider.setLastName(updateRequest.getLastName());
            System.out.println("DEBUG: Updated lastName: " + updateRequest.getLastName());
        }
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().isEmpty()) {
            provider.setEmail(updateRequest.getEmail().trim().toLowerCase());
            System.out.println("DEBUG: Updated email: " + updateRequest.getEmail());
        }
        if (updateRequest.getPhoneNumber() != null) {
            provider.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getGender() != null) {
            provider.setGender(updateRequest.getGender());
        }
        if (updateRequest.getDateOfBirth() != null) {
            provider.setDateOfBirth(convertStringToDate(updateRequest.getDateOfBirth()));
        }
        if (updateRequest.getAddress() != null) {
            provider.setAddress(updateRequest.getAddress());
        }
        if (updateRequest.getCity() != null) {
            provider.setCity(updateRequest.getCity());
        }
        if (updateRequest.getCountry() != null) {
            provider.setCountry(updateRequest.getCountry());
        }
        
        // Professional Information
        if (updateRequest.getConsultationFee() != null) {
            provider.setConsultationFee(updateRequest.getConsultationFee());
        }
        if (updateRequest.getBio() != null) {
            provider.setBio(updateRequest.getBio());
        }
        if (updateRequest.getClinicName() != null) {
            provider.setClinicName(updateRequest.getClinicName());
        }
        if (updateRequest.getLicenseNumber() != null) {
            provider.setLicenseNumber(updateRequest.getLicenseNumber());
        }
        if (updateRequest.getAvailableDays() != null) {
            provider.setAvailableDays(updateRequest.getAvailableDays());
        }
        if (updateRequest.getAvailableTimeStart() != null) {
            provider.setAvailableTimeStart(updateRequest.getAvailableTimeStart());
        }
        if (updateRequest.getAvailableTimeEnd() != null) {
            provider.setAvailableTimeEnd(updateRequest.getAvailableTimeEnd());
        }
        if (updateRequest.getSpecializations() != null) {
            List<SpecializationType> specializationTypes = updateRequest.getSpecializations().stream()
                    .map(spec -> {
                        try {
                            return SpecializationType.valueOf(spec);
                        } catch (IllegalArgumentException e) {
                            System.out.println("Invalid specialization: " + spec);
                            return null;
                        }
                    })
                    .filter(spec -> spec != null)
                    .collect(Collectors.toList());
            provider.setSpecializations(specializationTypes);
        }
        
        // Education and Work Experience - Update the collections
        if (updateRequest.getEducationHistories() != null) {
            // Initialize collection if null
            if (provider.getEducationHistories() == null) {
                provider.setEducationHistories(new java.util.ArrayList<>());
            }
            // Clear existing education histories and add new ones
            provider.getEducationHistories().clear();
            for (com.MediConnect.EntryRelated.dto.healthprovider.EducationHistoryDTO eduDto : updateRequest.getEducationHistories()) {
                EducationHistory education = new EducationHistory();
                education.setInstitutionName(eduDto.getInstitutionName());
                education.setStartDate(convertStringToDate(eduDto.getStartDate()));
                education.setEndDate(convertStringToDate(eduDto.getEndDate()));
                education.setStillEnrolled(eduDto.isStillEnrolled());
                education.setProvider(provider);
                provider.getEducationHistories().add(education);
            }
        }
        
        if (updateRequest.getWorkExperiences() != null) {
            // Initialize collection if null
            if (provider.getWorkExperiences() == null) {
                provider.setWorkExperiences(new java.util.ArrayList<>());
            }
            // Clear existing work experiences and add new ones
            provider.getWorkExperiences().clear();
            for (com.MediConnect.EntryRelated.dto.healthprovider.WorkExperienceDTO workDto : updateRequest.getWorkExperiences()) {
                WorkExperience work = new WorkExperience();
                work.setOrganizationName(workDto.getOrganizationName());
                work.setRoleTitle(workDto.getRoleTitle());
                work.setStartDate(convertStringToDate(workDto.getStartDate()));
                work.setEndDate(convertStringToDate(workDto.getEndDate()));
                work.setStillWorking(workDto.isStillWorking());
                work.setProvider(provider);
                provider.getWorkExperiences().add(work);
            }
        }
    }

    private Date convertStringToDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            String cleanDate = dateString.trim();
            // If ISO format (contains T), extract just the date part
            if (cleanDate.contains("T")) {
                cleanDate = cleanDate.substring(0, cleanDate.indexOf("T")).trim();
            }
            // Parse the date in simple yyyy-MM-dd format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            return sdf.parse(cleanDate);
        } catch (Exception e) {
            System.out.println("Error parsing date: '" + dateString + "'");
            e.printStackTrace();
            return null;
        }
    }
}