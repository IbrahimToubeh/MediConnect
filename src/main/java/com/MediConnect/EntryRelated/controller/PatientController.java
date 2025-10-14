package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.patient.*;
import com.MediConnect.EntryRelated.dto.ChangePasswordRequestDTO;
import com.MediConnect.EntryRelated.dto.NotificationPreferencesDTO;
import com.MediConnect.EntryRelated.dto.PrivacySettingsDTO;
import com.MediConnect.EntryRelated.entities.LaboratoryResult;
import com.MediConnect.EntryRelated.entities.Medication;
import com.MediConnect.EntryRelated.entities.MentalHealthMedication;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.config.JWTService;
import com.MediConnect.EntryRelated.repository.LabResultRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.service.patient.PatientService;
import com.MediConnect.EntryRelated.service.OTPService;
import com.MediConnect.EntryRelated.service.ActivityService;
import com.MediConnect.EntryRelated.service.NotificationPreferencesService;
import com.MediConnect.EntryRelated.service.PrivacySettingsService;
import com.MediConnect.Service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final UserService userService;
    private final PatientRepo patientRepo;
    private final LabResultRepo labResultRepo;
    private final JWTService jwtService;
    private final OTPService otpService;
    private final ActivityService activityService;
    private final NotificationPreferencesService notificationPreferencesService;
    private final PrivacySettingsService privacySettingsService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody SignupPatientRequestDTO patientInfo) {
        try {
            String result = patientService.register(patientInfo);
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

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginPatientRequestDTO patientInfo, HttpServletRequest request) {
        try {
            // First verify the user is a patient before authentication
            Patient patient = patientRepo.findByUsername(patientInfo.getUsername())
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            
            // Verify the user has PATIENT role
            if (!"PATIENT".equals(patient.getRole())) {
                throw new RuntimeException("Access denied: This account is not a patient account");
            }
            
            // Authenticate the user (verify username/password)
            userService.authenticate(patientInfo.getUsername(), patientInfo.getPassword());
            
            // Check if 2FA is enabled
            if (userService.isTwoFactorEnabled(patientInfo.getUsername())) {
                // Send OTP to email
                otpService.sendLoginOTP(patient.getEmail());
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "2fa_required");
                response.put("message", "OTP sent to your email. Please verify to complete login.");
                response.put("email", patient.getEmail());
                response.put("username", patientInfo.getUsername());
                return ResponseEntity.ok(response);
            }
            
            // If 2FA not enabled, generate token immediately
            String token = jwtService.generateToken(new com.MediConnect.config.UserPrincipal(patient));
            
            // Create login session and log activity
            activityService.createLoginSession(patient, token, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Patient login successful");
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/verify-login-otp")
    public ResponseEntity<Map<String, Object>> verifyLoginOTP(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String username = request.get("username");
            String otp = request.get("otp");
            
            Patient patient = patientRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            
            // Verify OTP
            if (!otpService.verifyLoginOTP(patient.getEmail(), otp)) {
                throw new RuntimeException("Invalid or expired OTP");
            }
            
            // Clear the OTP
            otpService.clearLoginOTP(patient.getEmail());
            
            // Generate token
            String token = jwtService.generateToken(new com.MediConnect.config.UserPrincipal(patient));
            
            // Create login session and log activity
            activityService.createLoginSession(patient, token, httpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Login successful");
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestHeader("Authorization") String token) {
        try {
            // Extract username from JWT token
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            
            // Find patient by username
            Patient patient = patientRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            
            // Build response with patient data
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", buildPatientProfileResponseDTO(patient));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/lab-results")
    public ResponseEntity<Map<String, Object>> getLabResults(@RequestHeader("Authorization") String token) {
        try {
            // Extract username from JWT token
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            
            // Find patient by username
            Patient patient = patientRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            
            // Get lab results for this patient
            List<LaboratoryResult> labResults = labResultRepo.findByPatientId(patient.getId());
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", labResults.stream().map(this::buildLabResultResponse).collect(java.util.stream.Collectors.toList()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    private PatientProfileResponseDTO buildPatientProfileResponseDTO(Patient patient) {
        PatientProfileResponseDTO profile = new PatientProfileResponseDTO();
        
        // Basic Information
        profile.setId(patient.getId());
        profile.setUsername(patient.getUsername());
        profile.setEmail(patient.getEmail());
        profile.setFirstName(patient.getFirstName());
        profile.setLastName(patient.getLastName());
        profile.setGender(patient.getGender());
        profile.setDateOfBirth(patient.getDateOfBirth());
        profile.setPhoneNumber(patient.getPhoneNumber());
        profile.setRegistrationDate(patient.getRegistrationDate());
        
        // Physical Information
        profile.setHeight(patient.getHeight());
        profile.setWeight(patient.getWeight());
        profile.setBloodType(patient.getBloodType() != null ? patient.getBloodType().toString() : null);
        
        // Medical Information
        profile.setAllergies(patient.getAllergies());
        profile.setMedicalConditions(patient.getMedicalConditions());
        profile.setPreviousSurgeries(patient.getPreviousSurgeries());
        profile.setFamilyMedicalHistory(patient.getFamilyMedicalHistory());
        
        // Lifestyle Information
        profile.setDietaryHabits(patient.getDietaryHabits() != null ? patient.getDietaryHabits().toString() : null);
        profile.setAlcoholConsumption(patient.getAlcoholConsumption() != null ? patient.getAlcoholConsumption().toString() : null);
        profile.setPhysicalActivity(patient.getPhysicalActivity() != null ? patient.getPhysicalActivity().toString() : null);
        profile.setSmokingStatus(patient.getSmokingStatus() != null ? patient.getSmokingStatus().toString() : null);
        profile.setMentalHealthCondition(patient.getMentalHealthCondition() != null ? patient.getMentalHealthCondition().toString() : null);
        
        // Medications - Convert entities to DTOs
        if (patient.getMedications() != null) {
            profile.setMedications(patient.getMedications().stream()
                .map(this::convertToMedicationDTO)
                .collect(java.util.stream.Collectors.toList()));
        } else {
            profile.setMedications(new java.util.ArrayList<>());
        }
        
        if (patient.getMentalHealthMedications() != null) {
            profile.setMentalHealthMedications(patient.getMentalHealthMedications().stream()
                .map(this::convertToMentalHealthMedicationDTO)
                .collect(java.util.stream.Collectors.toList()));
        } else {
            profile.setMentalHealthMedications(new java.util.ArrayList<>());
        }
        
        // Lab Results
        try {
            List<LaboratoryResult> labResults = labResultRepo.findByPatientId(patient.getId());
            profile.setLabResults(labResults.stream()
                .map(this::convertToLabResultDTO)
                .collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            // If lab results table doesn't exist, set empty list
            profile.setLabResults(new java.util.ArrayList<>());
        }
        
        // Insurance Information
        profile.setInsuranceProvider(patient.getInsuranceProvider());
        profile.setInsuranceNumber(patient.getInsuranceNumber());
        
        return profile;
    }

    private MedicationResponseDTO convertToMedicationDTO(Medication medication) {
        MedicationResponseDTO dto = new MedicationResponseDTO();
        dto.setId(medication.getId());
        dto.setMedicationName(medication.getMedicationName());
        dto.setMedicationDosage(medication.getMedicationDosage());
        dto.setMedicationFrequency(medication.getMedicationFrequency());
        dto.setMedicationStartDate(medication.getMedicationStartDate());
        dto.setMedicationEndDate(medication.getMedicationEndDate());
        dto.setInUse(medication.isInUse());
        return dto;
    }

    private MentalHealthMedicationResponseDTO convertToMentalHealthMedicationDTO(MentalHealthMedication medication) {
        MentalHealthMedicationResponseDTO dto = new MentalHealthMedicationResponseDTO();
        dto.setId(medication.getId());
        dto.setMedicationName(medication.getMedicationName());
        dto.setMedicationDosage(medication.getMedicationDosage());
        dto.setMedicationFrequency(medication.getMedicationFrequency());
        dto.setMedicationStartDate(medication.getMedicationStartDate());
        dto.setMedicationEndDate(medication.getMedicationEndDate());
        dto.setInUse(medication.isInUse());
        return dto;
    }

    private LabResultResponseDTO convertToLabResultDTO(LaboratoryResult labResult) {
        LabResultResponseDTO dto = new LabResultResponseDTO();
        dto.setId(labResult.getId());
        dto.setDescription(labResult.getDescription());
        dto.setHasImage(labResult.getImage() != null && labResult.getImage().length > 0);
        dto.setImageSize(labResult.getImage() != null ? labResult.getImage().length : 0);
        return dto;
    }

    private Map<String, Object> buildLabResultResponse(LaboratoryResult labResult) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", labResult.getId());
        result.put("description", labResult.getDescription());
        result.put("hasImage", labResult.getImage() != null && labResult.getImage().length > 0);
        result.put("imageSize", labResult.getImage() != null ? labResult.getImage().length : 0);
        return result;
    }

    @GetMapping("/lab-result/{id}/image")
    public ResponseEntity<byte[]> getLabResultImage(@PathVariable Long id) {
        try {
            LaboratoryResult labResult = labResultRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Lab result not found"));
            
            if (labResult.getImage() == null || labResult.getImage().length == 0) {
                return ResponseEntity.notFound().build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(labResult.getImage().length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(labResult.getImage());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/upload-lab-result")
    public ResponseEntity<Map<String, String>> uploadLabResult(
            @RequestParam("patientId") Long patientId,
            @RequestParam("description") String description,
            @RequestParam("image") MultipartFile imageFile
    ) {
        try {
            if (imageFile.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Image file is required");
                return ResponseEntity.badRequest().body(response);
            }

            LaboratoryResult result = new LaboratoryResult();
            result.setDescription(description);
            result.setPatient(patientRepo.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found")));
            result.setImage(imageFile.getBytes());
            labResultRepo.save(result);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Lab result uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Invalid image file");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdatePatientProfileRequestDTO updateRequest) {
        try {
            System.out.println("PUT /patient/profile endpoint called");
            // Extract username from JWT token
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);
            System.out.println("Username extracted: " + username);
            
            // Find patient by username
            Patient patient = patientRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            
            // Update patient fields
            updatePatientFields(patient, updateRequest);
            
            // Save updated patient
            System.out.println("Saving patient with medications: " + patient.getMedications().size());
            System.out.println("Saving patient with mental health medications: " + patient.getMentalHealthMedications().size());
            Patient savedPatient = patientRepo.save(patient);
            System.out.println("Patient saved successfully with ID: " + savedPatient.getId());
            
            // Build response with updated patient data
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Profile updated successfully");
            response.put("data", buildPatientProfileResponseDTO(patient));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    private void updatePatientFields(Patient patient, UpdatePatientProfileRequestDTO updateRequest) {
        // Basic Information
        if (updateRequest.getFirstName() != null) {
            patient.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            patient.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getEmail() != null) {
            patient.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getPhoneNumber() != null) {
            patient.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getGender() != null) {
            patient.setGender(updateRequest.getGender());
        }
        if (updateRequest.getDateOfBirth() != null) {
            patient.setDateOfBirth(updateRequest.getDateOfBirth());
        }
        
        // Physical Information
        if (updateRequest.getHeight() != null) {
            patient.setHeight(updateRequest.getHeight());
        }
        if (updateRequest.getWeight() != null) {
            patient.setWeight(updateRequest.getWeight());
        }
        if (updateRequest.getBloodType() != null) {
            try {
                patient.setBloodType(com.MediConnect.EntryRelated.entities.enums.BloodType.valueOf(updateRequest.getBloodType()));
            } catch (IllegalArgumentException e) {
                // Invalid blood type, skip update
            }
        }
        
        // Medical Information
        if (updateRequest.getAllergies() != null) {
            patient.setAllergies(updateRequest.getAllergies());
        }
        if (updateRequest.getMedicalConditions() != null) {
            patient.setMedicalConditions(updateRequest.getMedicalConditions());
        }
        if (updateRequest.getPreviousSurgeries() != null) {
            patient.setPreviousSurgeries(updateRequest.getPreviousSurgeries());
        }
        if (updateRequest.getFamilyMedicalHistory() != null) {
            patient.setFamilyMedicalHistory(updateRequest.getFamilyMedicalHistory());
        }
        
        // Lifestyle Information
        if (updateRequest.getDietaryHabits() != null) {
            try {
                patient.setDietaryHabits(com.MediConnect.EntryRelated.entities.enums.DietaryHabits.valueOf(updateRequest.getDietaryHabits()));
            } catch (IllegalArgumentException e) {
                // Invalid dietary habits, skip update
            }
        }
        if (updateRequest.getAlcoholConsumption() != null) {
            try {
                patient.setAlcoholConsumption(com.MediConnect.EntryRelated.entities.enums.AlcoholConsumption.valueOf(updateRequest.getAlcoholConsumption()));
            } catch (IllegalArgumentException e) {
                // Invalid alcohol consumption, skip update
            }
        }
        if (updateRequest.getPhysicalActivity() != null) {
            try {
                patient.setPhysicalActivity(com.MediConnect.EntryRelated.entities.enums.PhysicalActivity.valueOf(updateRequest.getPhysicalActivity()));
            } catch (IllegalArgumentException e) {
                // Invalid physical activity, skip update
            }
        }
        if (updateRequest.getSmokingStatus() != null) {
            try {
                patient.setSmokingStatus(com.MediConnect.EntryRelated.entities.enums.SmokingStatus.valueOf(updateRequest.getSmokingStatus()));
            } catch (IllegalArgumentException e) {
                // Invalid smoking status, skip update
            }
        }
        if (updateRequest.getMentalHealthCondition() != null) {
            try {
                patient.setMentalHealthCondition(com.MediConnect.EntryRelated.entities.enums.MentalHealthCondition.valueOf(updateRequest.getMentalHealthCondition()));
            } catch (IllegalArgumentException e) {
                // Invalid mental health condition, skip update
            }
        }
        
        // Insurance Information
        if (updateRequest.getInsuranceProvider() != null) {
            patient.setInsuranceProvider(updateRequest.getInsuranceProvider());
        }
        if (updateRequest.getInsuranceNumber() != null) {
            patient.setInsuranceNumber(updateRequest.getInsuranceNumber());
        }
        
        // Medications and Lab Results - Update the collections
        if (updateRequest.getMedications() != null) {
            System.out.println("Updating medications: " + updateRequest.getMedications().size() + " items");
            // Clear existing medications and add new ones
            patient.getMedications().clear();
            for (CurrentMedicationDTO medDto : updateRequest.getMedications()) {
                Medication medication = new Medication();
                medication.setMedicationName(medDto.getMedicationName());
                medication.setMedicationDosage(medDto.getMedicationDosage());
                medication.setMedicationFrequency(medDto.getMedicationFrequency());
                medication.setMedicationStartDate(medDto.getMedicationStartDate());
                medication.setMedicationEndDate(medDto.getMedicationEndDate());
                medication.setInUse(medDto.isInUse());
                medication.setPatient(patient);
                patient.getMedications().add(medication);
                System.out.println("Added medication: " + medDto.getMedicationName());
            }
        }
        
        if (updateRequest.getMentalHealthMedications() != null) {
            System.out.println("Updating mental health medications: " + updateRequest.getMentalHealthMedications().size() + " items");
            // Clear existing mental health medications and add new ones
            patient.getMentalHealthMedications().clear();
            for (MentalHealthMedicationDTO medDto : updateRequest.getMentalHealthMedications()) {
                MentalHealthMedication medication = new MentalHealthMedication();
                medication.setMedicationName(medDto.getMedicationName());
                medication.setMedicationDosage(medDto.getMedicationDosage());
                medication.setMedicationFrequency(medDto.getMedicationFrequency());
                medication.setMedicationStartDate(medDto.getMedicationStartDate());
                medication.setMedicationEndDate(medDto.getMedicationEndDate());
                medication.setInUse(medDto.isInUse());
                medication.setPatient(patient);
                patient.getMentalHealthMedications().add(medication);
                System.out.println("Added mental health medication: " + medDto.getMedicationName());
            }
        }
        
        if (updateRequest.getLabResults() != null) {
            // Clear existing lab results and add new ones
            patient.getLaboratoryResults().clear();
            for (LabResultResponseDTO labDto : updateRequest.getLabResults()) {
                LaboratoryResult labResult = new LaboratoryResult();
                labResult.setDescription(labDto.getDescription());
                labResult.setPatient(patient);
                // Note: Image handling would need additional implementation
                patient.getLaboratoryResults().add(labResult);
            }
        }
    }

    @PostMapping("/edit")
    public ResponseEntity<Map<String, Object>> updateProfilePost(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdatePatientProfileRequestDTO updateRequest) {
        System.out.println("POST /patient/edit endpoint called");
        return updateProfile(token, updateRequest);
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        System.out.println("GET /patient/test endpoint called");
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Patient controller is working");
        return ResponseEntity.ok(response);
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
            
            // Verify user is a patient
            Patient patient = patientRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            
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
    public ResponseEntity<Map<String, Object>> getActivity(Authentication authentication) {
        try {
            String username = authentication.getName();
            Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("sessions", activityService.getLoginSessions(patient));
            response.put("activities", activityService.getAccountActivities(patient, 50));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/notification-preferences")
    public ResponseEntity<Map<String, Object>> getNotificationPreferences(Authentication authentication) {
        try {
            String username = authentication.getName();
            Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("preferences", notificationPreferencesService.getNotificationPreferences(patient));
            
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
            Authentication authentication,
            @RequestBody NotificationPreferencesDTO preferences) {
        try {
            String username = authentication.getName();
            Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notification preferences updated successfully");
            response.put("preferences", notificationPreferencesService.updateNotificationPreferences(patient, preferences));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/privacy-settings")
    public ResponseEntity<Map<String, Object>> getPrivacySettings(Authentication authentication) {
        try {
            String username = authentication.getName();
            Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("settings", privacySettingsService.getPrivacySettings(patient));
            
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
            Authentication authentication,
            @RequestBody PrivacySettingsDTO settings) {
        try {
            String username = authentication.getName();
            Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Privacy settings updated successfully");
            response.put("settings", privacySettingsService.updatePrivacySettings(patient, settings));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
