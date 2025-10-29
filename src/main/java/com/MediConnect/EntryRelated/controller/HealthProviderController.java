package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.healthprovider.GetAllSpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.LoginHPRequestDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.dto.ChangePasswordRequestDTO;
import com.MediConnect.EntryRelated.dto.NotificationPreferencesDTO;
import com.MediConnect.EntryRelated.dto.PrivacySettingsDTO;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import com.MediConnect.EntryRelated.entities.SpecializationType;
import com.MediConnect.EntryRelated.service.healthprovider.HealthcareProviderService;
import com.MediConnect.EntryRelated.service.OTPService;
import com.MediConnect.EntryRelated.service.ActivityService;
import com.MediConnect.EntryRelated.service.NotificationPreferencesService;
import com.MediConnect.EntryRelated.service.PrivacySettingsService;
import com.MediConnect.Service.UserService;
import com.MediConnect.config.JWTService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
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
    private final OTPService otpService;
    private final ActivityService activityService;
    private final NotificationPreferencesService notificationPreferencesService;
    private final PrivacySettingsService privacySettingsService;


    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginHPRequestDTO loginRequest,
            HttpServletRequest request) {
        try {
            Map<String, Object> response = healthcareProviderService.loginProvider(loginRequest, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/verify-login-otp")
    public ResponseEntity<Map<String, Object>> verifyLoginOTP(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            Map<String, Object> response = healthcareProviderService.verifyLoginOTP(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
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

    @GetMapping("/public-profile/{id}")
    public ResponseEntity<Map<String, Object>> getPublicProfile(@PathVariable Long id) {
        try {
            // Find healthcare provider by ID
            HealthcareProvider provider = healthcareProviderService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));
            
            // Check if profile is public
            boolean isPublic = privacySettingsService.isProfilePublic(provider);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            
            if (!isPublic) {
                // Profile is private
                response.put("isPrivate", true);
                response.put("message", "This profile is private");
                // Only return basic info
                Map<String, Object> basicInfo = new HashMap<>();
                basicInfo.put("id", provider.getId());
                basicInfo.put("firstName", provider.getFirstName());
                basicInfo.put("lastName", provider.getLastName());
                basicInfo.put("profilePicture", provider.getProfilePicture());
                response.put("data", basicInfo);
            } else {
                // Profile is public - apply individual privacy settings
                response.put("isPrivate", false);
                response.put("data", buildHealthcareProviderProfileResponseDTOWithPrivacy(provider));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
        profile.put("insuranceAccepted", provider.getInsuranceAccepted() != null ? provider.getInsuranceAccepted() : new java.util.ArrayList<>());
        profile.put("profilePicture", provider.getProfilePicture());
        profile.put("bannerPicture", provider.getBannerPicture());
        
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
        if (updateRequest.getInsuranceAccepted() != null) {
            provider.setInsuranceAccepted(updateRequest.getInsuranceAccepted());
            System.out.println("DEBUG: Updated insuranceAccepted: " + updateRequest.getInsuranceAccepted());
        }
        if (updateRequest.getProfilePicture() != null) {
            provider.setProfilePicture(updateRequest.getProfilePicture());
        }
        if (updateRequest.getBannerPicture() != null) {
            provider.setBannerPicture(updateRequest.getBannerPicture());
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

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchDoctors(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String insurance,
            @RequestParam(required = false) Double minFee,
            @RequestParam(required = false) Double maxFee,
            @RequestParam(required = false) Double minRating
    ) {
        try {
            System.out.println("DEBUG SEARCH: name=" + name + ", city=" + city + ", specialty=" + specialty + 
                             ", insurance=" + insurance + ", minFee=" + minFee + ", maxFee=" + maxFee + ", minRating=" + minRating);
            
            List<HealthcareProvider> providers = healthcareProviderService.searchDoctors(
                name, city, specialty, insurance, minFee, maxFee, minRating
            );
            
            System.out.println("DEBUG SEARCH: Found " + providers.size() + " providers");
            
            // Convert to response DTOs
            List<Map<String, Object>> response = providers.stream()
                .map(provider -> {
                    Map<String, Object> providerMap = new HashMap<>();
                    providerMap.put("id", provider.getId());
                    providerMap.put("firstName", provider.getFirstName());
                    providerMap.put("lastName", provider.getLastName());
                    providerMap.put("profilePicture", provider.getProfilePicture());
                    providerMap.put("specializations", provider.getSpecializations());
                    providerMap.put("city", provider.getCity());
                    providerMap.put("clinicName", provider.getClinicName());
                    providerMap.put("consultationFee", provider.getConsultationFee());
                    providerMap.put("insuranceAccepted", provider.getInsuranceAccepted());
                    providerMap.put("bio", provider.getBio());
                    
                    // Add mock rating (consistent with search filter logic)
                    double mockRating = 3.5 + (provider.getId() % 15) * 0.1; // Ratings between 3.5-5.0
                    providerMap.put("rating", mockRating);
                    
                    return providerMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERROR SEARCH: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordRequestDTO changePasswordRequest) {
        try {
            // Validate that new password and confirm password match
            if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "New password and confirm password do not match");
                return ResponseEntity.badRequest().body(response);
            }

            // Extract username from JWT token
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            
            // Verify user is a healthcare provider
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));
            
            // Change password using UserService
            userService.changePassword(username, changePasswordRequest.getCurrentPassword(), changePasswordRequest.getNewPassword());
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to change password. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/enable-2fa")
    public ResponseEntity<Map<String, String>> enableTwoFactor(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            
            userService.enableTwoFactor(username);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Two-factor authentication enabled successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/disable-2fa")
    public ResponseEntity<Map<String, String>> disableTwoFactor(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            
            userService.disableTwoFactor(username);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Two-factor authentication disabled successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/2fa-status")
    public ResponseEntity<Map<String, Object>> getTwoFactorStatus(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            
            boolean enabled = userService.isTwoFactorEnabled(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("twoFactorEnabled", enabled);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/activity")
    public ResponseEntity<Map<String, Object>> getActivity(org.springframework.security.core.Authentication authentication) {
        try {
            String username = authentication.getName();
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("sessions", activityService.getLoginSessions(provider));
            response.put("activities", activityService.getAccountActivities(provider, 50));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/notification-preferences")
    public ResponseEntity<Map<String, Object>> getNotificationPreferences(org.springframework.security.core.Authentication authentication) {
        try {
            String username = authentication.getName();
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("preferences", notificationPreferencesService.getNotificationPreferences(provider));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/notification-preferences")
    public ResponseEntity<Map<String, Object>> updateNotificationPreferences(
            org.springframework.security.core.Authentication authentication,
            @RequestBody NotificationPreferencesDTO preferences) {
        try {
            String username = authentication.getName();
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notification preferences updated successfully");
            response.put("preferences", notificationPreferencesService.updateNotificationPreferences(provider, preferences));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/privacy-settings")
    public ResponseEntity<Map<String, Object>> getPrivacySettings(org.springframework.security.core.Authentication authentication) {
        try {
            String username = authentication.getName();
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("settings", privacySettingsService.getPrivacySettings(provider));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/privacy-settings")
    public ResponseEntity<Map<String, Object>> updatePrivacySettings(
            org.springframework.security.core.Authentication authentication,
            @RequestBody PrivacySettingsDTO settings) {
        try {
            String username = authentication.getName();
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Privacy settings updated successfully");
            response.put("settings", privacySettingsService.updatePrivacySettings(provider, settings));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private Map<String, Object> buildHealthcareProviderProfileResponseDTOWithPrivacy(HealthcareProvider provider) {
        Map<String, Object> profile = new HashMap<>();
        
        // Get privacy settings
        var privacySettings = privacySettingsService.getPrivacySettings(provider);
        
        // Basic Information (always shown)
        profile.put("id", provider.getId());
        profile.put("username", provider.getUsername());
        profile.put("firstName", provider.getFirstName());
        profile.put("lastName", provider.getLastName());
        profile.put("gender", provider.getGender());
        profile.put("dateOfBirth", provider.getDateOfBirth());
        
        // Conditional information based on privacy settings
        if (privacySettings.getShowEmail()) {
            profile.put("email", provider.getEmail());
        }
        if (privacySettings.getShowPhone()) {
            profile.put("phoneNumber", provider.getPhoneNumber());
        }
        if (privacySettings.getShowAddress()) {
            profile.put("address", provider.getAddress());
            profile.put("city", provider.getCity());
            profile.put("country", provider.getCountry());
        }
        
        // Professional Information (always shown for public profiles)
        profile.put("consultationFee", provider.getConsultationFee());
        profile.put("bio", provider.getBio());
        profile.put("clinicName", provider.getClinicName());
        profile.put("licenseNumber", provider.getLicenseNumber());
        profile.put("availableDays", provider.getAvailableDays());
        profile.put("availableTimeStart", provider.getAvailableTimeStart());
        profile.put("availableTimeEnd", provider.getAvailableTimeEnd());
        profile.put("insuranceAccepted", provider.getInsuranceAccepted() != null ? provider.getInsuranceAccepted() : new java.util.ArrayList<>());
        profile.put("profilePicture", provider.getProfilePicture());
        profile.put("bannerPicture", provider.getBannerPicture());
        
        // Specializations (always shown)
        if (provider.getSpecializations() != null) {
            profile.put("specializations", provider.getSpecializations().stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toList()));
        } else {
            profile.put("specializations", new java.util.ArrayList<>());
        }
        
        // Education Histories (always shown for public profiles)
        if (provider.getEducationHistories() != null) {
            profile.put("educationHistories", provider.getEducationHistories().stream()
                .map(this::convertToEducationHistoryDTO)
                .collect(java.util.stream.Collectors.toList()));
        } else {
            profile.put("educationHistories", new java.util.ArrayList<>());
        }
        
        // Work Experiences (always shown for public profiles)
        if (provider.getWorkExperiences() != null) {
            profile.put("workExperiences", provider.getWorkExperiences().stream()
                .map(this::convertToWorkExperienceDTO)
                .collect(java.util.stream.Collectors.toList()));
        } else {
            profile.put("workExperiences", new java.util.ArrayList<>());
        }
        
        return profile;
    }
    /*
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginHPRequestDTO healthProviderInfo, HttpServletRequest request) {
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
            // Authenticate the user (verify username/password)
            userService.authenticate(healthProviderInfo.getUsername(), healthProviderInfo.getPassword());

            // Check if 2FA is enabled
            if (userService.isTwoFactorEnabled(healthProviderInfo.getUsername())) {
                // Send OTP to email
                otpService.sendLoginOTP(provider.getEmail());

                Map<String, Object> response = new HashMap<>();
                response.put("status", "2fa_required");
                response.put("message", "OTP sent to your email. Please verify to complete login.");
                response.put("email", provider.getEmail());
                response.put("username", healthProviderInfo.getUsername());
                response.put("userId", provider.getId());
                return ResponseEntity.ok(response);
            }

            // If 2FA not enabled, generate token immediately
            String token = jwtService.generateToken(new com.MediConnect.config.UserPrincipal(provider));

            // Create login session and log activity
            activityService.createLoginSession(provider, token, request);

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

     */
    /*
    @PostMapping("/verify-login-otp")
    public ResponseEntity<Map<String, Object>> verifyLoginOTP(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String username = request.get("username");
            String otp = request.get("otp");

            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            // Verify OTP
            if (!otpService.verifyLoginOTP(provider.getEmail(), otp)) {
                throw new RuntimeException("Invalid or expired OTP");
            }

            // Clear the OTP
            otpService.clearLoginOTP(provider.getEmail());

            // Generate token
            String token = jwtService.generateToken(new com.MediConnect.config.UserPrincipal(provider));

            // Create login session and log activity
            activityService.createLoginSession(provider, token, httpRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("userId", provider.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
     */
}