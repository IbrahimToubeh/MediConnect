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
import com.MediConnect.socialmedia.service.CloudinaryService;
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
    private final CloudinaryService cloudinaryService;
    private final com.MediConnect.EntryRelated.repository.HealthcareProviderRepo healthcareProviderRepo;
    private final com.MediConnect.Repos.UserRepo userRepo;

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
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginPatientRequestDTO patientInfo, HttpServletRequest request) {
        try {
            Map<String, Object> response = patientService.login(patientInfo, request);
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
            Map<String, Object> response = patientService.verifyLoginOTP(request, httpRequest);
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
            Map<String, Object> response = patientService.getProfile(token);
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
            Map<String, Object> response = patientService.getLabResults(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    @GetMapping("/lab-result/{id}/image")
    public ResponseEntity<byte[]> getLabResultImage(@PathVariable Long id) {
        byte[] image = patientService.getLabResultImage(id);

        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(image.length);

        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }

    @PostMapping("/upload-lab-result")
    public ResponseEntity<Map<String, String>> uploadLabResult(
            @RequestParam("patientId") Long patientId,
            @RequestParam("description") String description,
            @RequestParam("image") MultipartFile imageFile
    ) {
        try {
            Map<String, String> response = patientService.uploadLabResult(patientId, description, imageFile);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
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
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);

            Map<String, Object> response = patientService.updateProfile(username, updateRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    //todo: ask abd about this , doesn't this just send to the other endpoint?
    @PostMapping("/edit")
    public ResponseEntity<Map<String, Object>> updateProfilePost(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdatePatientProfileRequestDTO updateRequest) {
        System.out.println("POST /patient/edit endpoint called");
        return updateProfile(token, updateRequest);
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestHeader("Authorization") String token
            ,@Valid @RequestBody ChangePasswordRequestDTO changePasswordRequest) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String username = jwtService.extractUserName(jwtToken);

            Map<String, String> response = patientService.changePassword(username, changePasswordRequest);
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

            Map<String, String> response = patientService.enableTwoFactor(username);
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

            Map<String, String> response = patientService.disableTwoFactor(username);
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

            Map<String, Object> response = patientService.getTwoFactorStatus(username);
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
        String username = authentication.getName();
        Map<String, Object> response = patientService.getActivity(username);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/notification-preferences")
    public ResponseEntity<Map<String, Object>> getNotificationPreferences(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(
                notificationPreferencesService.getNotificationPreferencesByUsername(username)
        );
    }

    @PutMapping("/notification-preferences")
    public ResponseEntity<Map<String, Object>> updateNotificationPreferences(
            Authentication authentication,
            @RequestBody NotificationPreferencesDTO preferences) {
        String username = authentication.getName();
        return ResponseEntity.ok(
                notificationPreferencesService.updateNotificationPreferencesByUsername(username, preferences)
        );
    }

    @GetMapping("/privacy-settings")
    public ResponseEntity<Map<String, Object>> getPrivacySettings(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(
                privacySettingsService.getPrivacySettingsByUsername(username)
        );
    }

    @PutMapping("/privacy-settings")
    public ResponseEntity<Map<String, Object>> updatePrivacySettings(
            Authentication authentication,
            @RequestBody PrivacySettingsDTO settings) {
        String username = authentication.getName();
        return ResponseEntity.ok(
                privacySettingsService.updatePrivacySettingsByUsername(username, settings)
        );
    }

    /*
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
    }*/
    /*

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
    } */


    /* @PostMapping("/verify-login-otp")
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
    }*/
    /*@GetMapping("/profile")
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
    }*/

    /*@GetMapping("/lab-results")
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
    }*/

    /*@GetMapping("/lab-result/{id}/image")
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
    }*/
    /*@GetMapping("/lab-result/{id}/image")
    public ResponseEntity<byte[]> getLabResultImage(@PathVariable Long id) {
        try {
            byte[] image = patientService.getLabResultImage(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(image.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(image);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }*/



    /*@PostMapping("/upload-lab-result")
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
    }*/
    /*@PutMapping("/profile")
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
    /* private void updatePatientFields(Patient patient, UpdatePatientProfileRequestDTO updateRequest) {
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
    }*/
    /*@PostMapping("/enable-2fa")
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
    }*/
    /* @PostMapping("/disable-2fa")
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
*/
    /*@GetMapping("/2fa-status")
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
    }*/

    /* @GetMapping("/activity")
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
*/
    /*@GetMapping("/activity")
    public ResponseEntity<Map<String, Object>> getActivity(Authentication authentication) {
        try {
            String username = authentication.getName();
            Map<String, Object> response = patientService.getActivity(username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }*/

    /**
     * Upload lab result PDF file
     * @param file The PDF file to upload
     * @param description Lab result description
     * @param authentication The authenticated user
     * @return Response with lab result details including resultUrl
     */
    @PostMapping("/lab-result/upload")
    public ResponseEntity<Map<String, Object>> uploadLabResult(
            @RequestParam("file") MultipartFile file,
            @RequestParam("description") String description,
            Authentication authentication
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

            // Get authenticated patient
            String username = authentication.getName();
            Patient patient = patientRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            // Save to DB directly (Bypassing Cloudinary)
            LaboratoryResult labResult = new LaboratoryResult();
            labResult.setDescription(description);
            labResult.setPatient(patient);
            labResult.setImage(file.getBytes());
            labResult.setHasImage(true);
            labResult.setImageSize((int) file.getSize());
            
            LaboratoryResult savedResult = labResultRepo.save(labResult);
            
            // Set URL to point to the local download endpoint
            // We need to save first to get the ID, then update the URL
            String resultUrl = "http://localhost:8080/patient/lab-result/" + savedResult.getId() + "/pdf";
            savedResult.setResultUrl(resultUrl);
            labResultRepo.save(savedResult);

            // Prepare response
            Map<String, Object> labResultData = new HashMap<>();
            labResultData.put("id", savedResult.getId());
            labResultData.put("description", savedResult.getDescription());
            labResultData.put("hasImage", savedResult.getHasImage());
            labResultData.put("imageSize", savedResult.getImageSize());
            labResultData.put("resultUrl", savedResult.getResultUrl());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Lab result uploaded successfully");
            response.put("data", labResultData);

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

    @GetMapping("/lab-result/{id}/pdf")
    public ResponseEntity<byte[]> getLabResultPdf(@PathVariable Long id) {
        LaboratoryResult labResult = labResultRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Lab result not found"));

        if (labResult.getImage() == null) {
             return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"lab-result.pdf\"")
                .body(labResult.getImage());
    }

    @DeleteMapping("/lab-result/{id}")
    public ResponseEntity<Map<String, String>> deleteLabResult(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            // Get authenticated patient
            String username = authentication.getName();
            Patient patient = patientRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            // Find lab result
            LaboratoryResult labResult = labResultRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Lab result not found"));

            // Verify ownership
            if (!labResult.getPatient().getId().equals(patient.getId())) {
                throw new RuntimeException("Access denied: You can only delete your own lab results");
            }

            // Delete from Cloudinary if it has a URL
            if (labResult.getResultUrl() != null) {
                // Extract public ID from URL (simplified logic, ideally CloudinaryService handles this)
                // For now, just delete from DB, Cloudinary file remains (orphaned) but harmless
                // TODO: Implement Cloudinary delete
            }

            // Delete from DB
            labResultRepo.delete(labResult);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Lab result deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
