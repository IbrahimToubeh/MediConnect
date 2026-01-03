package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.ChangePasswordRequestDTO;
import com.MediConnect.EntryRelated.dto.NotificationPreferencesDTO;
import com.MediConnect.EntryRelated.dto.PrivacySettingsDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.GetAllSpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.LoginHPRequestDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.ProfileView;
import com.MediConnect.EntryRelated.entities.SpecializationType;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import com.MediConnect.EntryRelated.service.ActivityService;
import com.MediConnect.EntryRelated.service.NotificationPreferencesService;
import com.MediConnect.EntryRelated.service.OTPService;
import com.MediConnect.EntryRelated.service.PrivacySettingsService;
import com.MediConnect.EntryRelated.service.healthprovider.HealthcareProviderService;
import com.MediConnect.EntryRelated.service.review.ReviewService;
import com.MediConnect.Service.UserService;
import com.MediConnect.socialmedia.service.CloudinaryService;
import com.MediConnect.config.JWTService;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.repository.ProfileViewRepository;
import com.MediConnect.EntryRelated.repository.DayAvailabilityRepository;
import com.MediConnect.EntryRelated.repository.BlockedTimeSlotRepository;
import com.MediConnect.EntryRelated.entities.DayAvailability;
import com.MediConnect.EntryRelated.entities.BlockedTimeSlot;
import com.MediConnect.EntryRelated.dto.healthprovider.ScheduleUpdateDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.DayAvailabilityDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.BlockedTimeSlotDTO;
import com.MediConnect.Repos.UserRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
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
    private final CloudinaryService cloudinaryService;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final ReviewService reviewService;
    private final ProfileViewRepository profileViewRepository;
    private final PatientRepo patientRepo;
    private final UserRepo userRepo;
    private final com.MediConnect.EntryRelated.repository.LabResultRepo labResultRepo;
    private final com.MediConnect.EntryRelated.repository.AppointmentRepository appointmentRepository;
    private final DayAvailabilityRepository dayAvailabilityRepository;
    private final BlockedTimeSlotRepository blockedTimeSlotRepository;

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


    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Check if username exists in any user table (patient, healthcare provider, or admin)
            boolean exists = patientRepo.existsByUsername(username) 
                    || healthcareProviderRepo.existsByUsername(username)
                    || userRepo.existsByUsername(username);
            
            response.put("status", "success");
            response.put("available", !exists);
            if (exists) {
                response.put("message", "Username already exists");
            } else {
                response.put("message", "Username is available");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error checking username availability");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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

    /**
     * Track a profile view for analytics.
     * 
     * This endpoint is called when someone views a doctor's public profile.
     * It records the view with a timestamp for analytics purposes.
     * 
     * @param id The ID of the healthcare provider whose profile was viewed
     * @return Success response
     */
    @PostMapping("/track-view/{id}")
    public ResponseEntity<Map<String, Object>> trackProfileView(@PathVariable Long id) {
        try {
            // Verify doctor exists
            if (!healthcareProviderRepo.existsById(id)) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Doctor not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Create and save a new profile view record
            ProfileView profileView = new ProfileView(id);
            profileViewRepository.save(profileView);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Profile view tracked");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
        profile.put("appointmentDurationMinutes", provider.getAppointmentDurationMinutes() != null 
                ? provider.getAppointmentDurationMinutes() : 30);
        
        // Include day availabilities for detailed schedule
        List<DayAvailability> dayAvailabilities = dayAvailabilityRepository.findByProvider(provider);
        List<Map<String, Object>> availabilityList = dayAvailabilities.stream()
                .map(av -> {
                    Map<String, Object> avMap = new HashMap<>();
                    avMap.put("dayOfWeek", av.getDayOfWeek());
                    avMap.put("enabled", av.getEnabled());
                    avMap.put("startTime", av.getStartTime());
                    avMap.put("endTime", av.getEndTime());
                    return avMap;
                })
                .collect(Collectors.toList());
        profile.put("dayAvailabilities", availabilityList);
        
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
            
            // Convert to response DTOs with real ratings
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
                    
                    // Fetch real rating from ReviewService
                    try {
                        Map<String, Object> ratingData = reviewService.getDoctorRating(provider.getId());
                        if ("success".equals(ratingData.get("status"))) {
                            Object avgRating = ratingData.get("averageRating");
                            double rating = (avgRating != null) ? ((Number) avgRating).doubleValue() : 0.0;
                            providerMap.put("rating", rating);
                        } else {
                            // If error, default to 0.0
                            providerMap.put("rating", 0.0);
                        }
                    } catch (Exception e) {
                        // If exception occurs, default to 0.0
                        System.out.println("Error fetching rating for doctor " + provider.getId() + ": " + e.getMessage());
                        providerMap.put("rating", 0.0);
                    }
                    
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
        profile.put("appointmentDurationMinutes", provider.getAppointmentDurationMinutes() != null 
                ? provider.getAppointmentDurationMinutes() : 30);
        
        // Include day availabilities for detailed schedule
        List<DayAvailability> dayAvailabilities = dayAvailabilityRepository.findByProvider(provider);
        List<Map<String, Object>> availabilityList = dayAvailabilities.stream()
                .map(av -> {
                    Map<String, Object> avMap = new HashMap<>();
                    avMap.put("dayOfWeek", av.getDayOfWeek());
                    avMap.put("enabled", av.getEnabled());
                    avMap.put("startTime", av.getStartTime());
                    avMap.put("endTime", av.getEndTime());
                    return avMap;
                })
                .collect(Collectors.toList());
        profile.put("dayAvailabilities", availabilityList);
        
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

    /**
     * Upload credential document PDF file
     * @param file The PDF file to upload
     * @param authentication The authenticated user
     * @return Response with updated provider details including licenseDocumentUrl
     */
    @PostMapping("/document/upload")
    public ResponseEntity<Map<String, Object>> uploadCredentialDocument(
            @RequestParam("file") MultipartFile file,
            org.springframework.security.core.Authentication authentication
    ) {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "File is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file type (PDF only)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Only PDF files are allowed");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "File size must be less than 5MB");
                return ResponseEntity.badRequest().body(response);
            }

            // Get authenticated healthcare provider
            String username = authentication.getName();
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));            if (provider == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Healthcare provider not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Upload file to Cloudinary
            String documentUrl = cloudinaryService.uploadFile(file, "mediconnect/doctor-credentials");

            // Update provider's license document URL
            provider.setLicenseDocumentUrl(documentUrl);
            healthcareProviderService.save(provider);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Credential document uploaded successfully");
            response.put("licenseDocumentUrl", documentUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PostMapping("/upload-license")
    public ResponseEntity<Map<String, Object>> uploadLicense(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String username = jwtService.extractUserName(token);
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Provider not found"));

            provider.setLicenseDocument(file.getBytes());
            provider.setLicenseDocumentContentType(file.getContentType());
            
            // Set URL to point to the local download endpoint
            String resultUrl = "http://localhost:8080/healthprovider/license/view";
            provider.setLicenseDocumentUrl(resultUrl);
            
            healthcareProviderRepo.save(provider);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "License uploaded successfully");
            response.put("url", resultUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
             Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/license/view")
    public ResponseEntity<byte[]> viewLicense(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String username = jwtService.extractUserName(token);
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Provider not found"));

            if (provider.getLicenseDocument() == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(provider.getLicenseDocumentContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"license.pdf\"")
                    .body(provider.getLicenseDocument());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint for doctors to view lab result PDFs from patient medical records.
     * Verifies that:
     * 1. Doctor is authenticated
     * 2. Lab result belongs to a patient who has an appointment with the doctor
     * 3. Medical records were shared for that appointment
     */
    @GetMapping("/appointment/{appointmentId}/lab-result/{labResultId}/pdf")
    public ResponseEntity<byte[]> getLabResultPdfForDoctor(
            @PathVariable Integer appointmentId,
            @PathVariable Long labResultId,
            HttpServletRequest request) {
        try {
            // Extract JWT token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String jwtToken = authHeader.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);

            // Get authenticated doctor
            HealthcareProvider doctor = healthcareProviderRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            // Get appointment
            com.MediConnect.Entities.AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));

            // Verify appointment belongs to this doctor
            if (!appointment.getHealthcareProvider().getId().equals(doctor.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Verify medical records were shared
            if (!Boolean.TRUE.equals(appointment.getShareMedicalRecords())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get lab result
            com.MediConnect.EntryRelated.entities.LaboratoryResult labResult = labResultRepo.findById(labResultId)
                    .orElseThrow(() -> new RuntimeException("Lab result not found"));

            // Verify lab result belongs to the patient in the appointment
            if (!labResult.getPatient().getId().equals(appointment.getPatient().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Check if PDF exists
            if (labResult.getImage() == null || labResult.getImage().length == 0) {
                return ResponseEntity.notFound().build();
            }

            // Return PDF
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"lab-result-" + labResultId + ".pdf\"")
                    .body(labResult.getImage());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== SCHEDULE MANAGEMENT ENDPOINTS ====================

    /**
     * Get doctor's schedule (day availabilities and appointment duration)
     */
    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Object>> getSchedule(
            @RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");

            // Get day availabilities
            List<DayAvailability> availabilities = dayAvailabilityRepository.findByProvider(provider);
            List<Map<String, Object>> availabilityList = availabilities.stream()
                    .map(av -> {
                        Map<String, Object> avMap = new HashMap<>();
                        avMap.put("dayOfWeek", av.getDayOfWeek());
                        avMap.put("enabled", av.getEnabled());
                        avMap.put("startTime", av.getStartTime());
                        avMap.put("endTime", av.getEndTime());
                        return avMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> schedule = new HashMap<>();
            schedule.put("dayAvailabilities", availabilityList);
            schedule.put("appointmentDurationMinutes", provider.getAppointmentDurationMinutes() != null 
                    ? provider.getAppointmentDurationMinutes() : 30);

            response.put("data", schedule);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update doctor's schedule (day availabilities and appointment duration)
     */
    @PutMapping("/schedule")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @RequestHeader("Authorization") String token,
            @RequestBody ScheduleUpdateDTO scheduleUpdate) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            // Update appointment duration
            if (scheduleUpdate.getAppointmentDurationMinutes() != null) {
                provider.setAppointmentDurationMinutes(scheduleUpdate.getAppointmentDurationMinutes());
                healthcareProviderService.save(provider);
            }

            // Update day availabilities
            if (scheduleUpdate.getDayAvailabilities() != null) {
                // Delete existing availabilities
                dayAvailabilityRepository.deleteByProvider(provider);
                // Flush to ensure delete is executed before inserts
                dayAvailabilityRepository.flush();

                // Create new availabilities
                for (DayAvailabilityDTO dto : scheduleUpdate.getDayAvailabilities()) {
                    DayAvailability availability = new DayAvailability();
                    availability.setProvider(provider);
                    availability.setDayOfWeek(dto.getDayOfWeek());
                    availability.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
                    availability.setStartTime(dto.getStartTime());
                    availability.setEndTime(dto.getEndTime());
                    dayAvailabilityRepository.save(availability);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Schedule updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get blocked time slots for a doctor
     */
    @GetMapping("/blocked-slots")
    public ResponseEntity<Map<String, Object>> getBlockedSlots(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            List<BlockedTimeSlot> blockedSlots;
            if (startDate != null && endDate != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date start = sdf.parse(startDate);
                Date end = sdf.parse(endDate);
                blockedSlots = blockedTimeSlotRepository.findByProviderAndDateRange(provider, start, end);
            } else {
                blockedSlots = blockedTimeSlotRepository.findByProvider(provider);
            }

            List<Map<String, Object>> slotList = blockedSlots.stream()
                    .map(slot -> {
                        Map<String, Object> slotMap = new HashMap<>();
                        slotMap.put("id", slot.getId());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        slotMap.put("date", sdf.format(slot.getBlockedDate()));
                        slotMap.put("startTime", slot.getStartTime());
                        slotMap.put("endTime", slot.getEndTime());
                        slotMap.put("reason", slot.getReason());
                        return slotMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", slotList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Add a blocked time slot
     */
    @PostMapping("/blocked-slots")
    public ResponseEntity<Map<String, Object>> addBlockedSlot(
            @RequestHeader("Authorization") String token,
            @RequestBody BlockedTimeSlotDTO blockedSlotDTO) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            BlockedTimeSlot blockedSlot = new BlockedTimeSlot();
            blockedSlot.setProvider(provider);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            blockedSlot.setBlockedDate(sdf.parse(blockedSlotDTO.getDate()));
            blockedSlot.setStartTime(blockedSlotDTO.getStartTime());
            
            // If end time is not provided, calculate it based on appointment duration
            String endTime = blockedSlotDTO.getEndTime();
            if (endTime == null || endTime.isEmpty()) {
                Integer duration = provider.getAppointmentDurationMinutes();
                if (duration == null) {
                    duration = 30; // Default to 30 minutes
                }
                
                // Calculate end time by adding duration to start time
                String[] startParts = blockedSlotDTO.getStartTime().split(":");
                int startHour = Integer.parseInt(startParts[0]);
                int startMinute = Integer.parseInt(startParts[1]);
                
                int totalMinutes = startHour * 60 + startMinute + duration;
                int endHour = totalMinutes / 60;
                int endMinute = totalMinutes % 60;
                
                // Handle overflow to next day (shouldn't happen in practice, but just in case)
                if (endHour >= 24) {
                    endHour = endHour % 24;
                }
                
                endTime = String.format("%02d:%02d", endHour, endMinute);
            }
            
            blockedSlot.setEndTime(endTime);
            blockedSlot.setReason(blockedSlotDTO.getReason());

            blockedTimeSlotRepository.save(blockedSlot);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Time slot blocked successfully");
            response.put("data", Map.of("id", blockedSlot.getId()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Remove a blocked time slot
     */
    @DeleteMapping("/blocked-slots/{id}")
    public ResponseEntity<Map<String, Object>> removeBlockedSlot(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider provider = healthcareProviderService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

            BlockedTimeSlot blockedSlot = blockedTimeSlotRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Blocked time slot not found"));

            // Verify ownership
            if (!blockedSlot.getProvider().getId().equals(provider.getId())) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            blockedTimeSlotRepository.delete(blockedSlot);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Blocked time slot removed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}