package com.MediConnect.EntryRelated.service.patient.impl;
//test
import com.MediConnect.EntryRelated.dto.ChangePasswordRequestDTO;
import com.MediConnect.EntryRelated.dto.NotificationPreferencesDTO;
import com.MediConnect.EntryRelated.dto.PrivacySettingsDTO;
import com.MediConnect.EntryRelated.dto.patient.*;
import com.MediConnect.EntryRelated.entities.LaboratoryResult;
import com.MediConnect.EntryRelated.entities.Medication;
import com.MediConnect.EntryRelated.entities.MentalHealthMedication;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.repository.LabResultRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.service.ActivityService;
import com.MediConnect.EntryRelated.service.NotificationPreferencesService;
import com.MediConnect.EntryRelated.service.OTPService;
import com.MediConnect.EntryRelated.service.patient.PatientService;
import com.MediConnect.EntryRelated.service.patient.mapper.LaboratoryResultMapper;
import com.MediConnect.EntryRelated.service.patient.mapper.PatientMapper;
import com.MediConnect.Service.UserService;
import com.MediConnect.EntryRelated.exception.AccountStatusException;
import com.MediConnect.config.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepo patientRepo;
    private final UserService userService;
    private final OTPService otpService;
    private final PatientMapper patientMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final ActivityService activityService;
    private final LabResultRepo labResultRepo;
    private final LaboratoryResultMapper labResultMapper;
    private final NotificationPreferencesService notificationPreferencesServiceCore; // existing logic

    @Override
    public String register(SignupPatientRequestDTO dto) {
        String normalizedEmail = dto.getEmail().trim().toLowerCase();

        if (patientRepo.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (patientRepo.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already exists");
        }

        Patient patient = patientMapper.signupDtoToPatient(dto);
        patient.setEmail(normalizedEmail);
        patient.setRegistrationDate(new Date());
        patient.setPassword(passwordEncoder.encode(patient.getPassword()));

        patientRepo.save(patient);
        otpService.clearRegistrationOTP(normalizedEmail);

        return "Patient registered successfully";
    }
    @Override
    public Map<String, Object> login(LoginPatientRequestDTO patientInfo, HttpServletRequest request) {
        // 1️⃣ Verify patient exists
        Patient patient = patientRepo.findByUsername(patientInfo.getUsername())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // 2️⃣ Verify patient role
        if (!"PATIENT".equals(patient.getRole())) {
            throw new RuntimeException("Access denied: This account is not a patient account");
        }

        // 3️⃣ Authenticate username/password
        try {
            userService.authenticate(patientInfo.getUsername(), patientInfo.getPassword());
        } catch (AccountStatusException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        Map<String, Object> response = new HashMap<>();

        // 4️⃣ Handle 2FA
        if (userService.isTwoFactorEnabled(patientInfo.getUsername())) {
            otpService.sendLoginOTP(patient.getEmail());
            response.put("status", "2fa_required");
            response.put("message", "OTP sent to your email. Please verify to complete login.");
            response.put("email", patient.getEmail());
            response.put("username", patientInfo.getUsername());
            return response;
        }

        // 5️⃣ If 2FA not enabled, generate JWT
        String token = jwtService.generateToken(new com.MediConnect.config.UserPrincipal(patient));

        // 6️⃣ Log login activity
        activityService.createLoginSession(patient, token, request);

        response.put("status", "success");
        response.put("message", "Patient login successful");
        response.put("token", token);
        return response;
    }
    @Override
    public Map<String, Object> verifyLoginOTP(Map<String, String> request, HttpServletRequest httpRequest) {
        String username = request.get("username");
        String otp = request.get("otp");

        // 1️⃣ Find the patient
        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // 2️⃣ Verify OTP
        if (!otpService.verifyLoginOTP(patient.getEmail(), otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // 3️⃣ Clear OTP
        otpService.clearLoginOTP(patient.getEmail());

        // 4️⃣ Generate token
        String token = jwtService.generateToken(new com.MediConnect.config.UserPrincipal(patient));

        // 5️⃣ Log session and activity
        activityService.createLoginSession(patient, token, httpRequest);

        // 6️⃣ Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Login successful");
        response.put("token", token);
        return response;
    }
    @Override
    public Map<String, Object> getProfile(String token) {
        // 1️⃣ Extract JWT (strip "Bearer " if included)
        String jwtToken = token.replace("Bearer ", "");
        String username = jwtService.extractUserName(jwtToken);

        // 2️⃣ Find the patient
        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // 3️⃣ Build the response DTO or map
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", buildPatientProfileResponseDTO(patient));

        return response;
    }
    @Override
    public Map<String, Object> getLabResults(String token) {
        String jwtToken = token.replace("Bearer ", "");
        String username = jwtService.extractUserName(jwtToken);

        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        List<LabResultResponseDTO> labResultsDTO = patient.getLaboratoryResults().stream()
                .map(labResultMapper::toDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", labResultsDTO);

        return response;
    }

    @Override
    public byte[] getLabResultImage(Long labResultId) {
        LaboratoryResult labResult = labResultRepo.findById(labResultId)
                .orElseThrow(() -> new RuntimeException("Lab result not found"));

        // Return null if image not present
        if (labResult.getImage() == null || labResult.getImage().length == 0) {
            return null;
        }

        return labResult.getImage();
    }
    @Override
    public Map<String, String> uploadLabResult(Long patientId, String description, MultipartFile imageFile) throws IOException {
        if (imageFile.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        LaboratoryResult result = new LaboratoryResult();
        result.setDescription(description);
        result.setPatient(patient);
        result.setImage(imageFile.getBytes());

        labResultRepo.save(result);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Lab result uploaded successfully");
        return response;
    }
    @Override
    public Map<String, Object> updateProfile(String username, UpdatePatientProfileRequestDTO updateRequest) {
        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Update patient fields
        updatePatientFields(patient, updateRequest);

        // Save updated patient
        Patient savedPatient = patientRepo.save(patient);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Profile updated successfully");
        response.put("data", buildPatientProfileResponseDTO(savedPatient));

        return response;
    }

    @Override
    public Map<String, String> changePassword(String username, ChangePasswordRequestDTO changePasswordRequest) {
        // Validate new password matches confirm password
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        // Verify user is a patient
        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Delegate password change to UserService
        userService.changePassword(username, changePasswordRequest.getCurrentPassword(), changePasswordRequest.getNewPassword());

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Password changed successfully");
        return response;
    }

    @Override
    public Map<String, String> enableTwoFactor(String username) {
        // Verify user exists
        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Delegate enabling 2FA to UserService
        userService.enableTwoFactor(username);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Two-factor authentication enabled successfully");
        return response;
    }
    @Override
    public Map<String, String> disableTwoFactor(String username) {
        // Verify user exists
        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Delegate disabling 2FA to UserService
        userService.disableTwoFactor(username);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Two-factor authentication disabled successfully");
        return response;
    }
    @Override
    public Map<String, Object> getTwoFactorStatus(String username) {
        // Verify user exists
        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        boolean enabled = userService.isTwoFactorEnabled(username);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("twoFactorEnabled", enabled);

        return response;
    }
    @Override
    public Map<String, Object> getActivity(String username) {
        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("sessions", activityService.getLoginSessions(patient));
        response.put("activities", activityService.getAccountActivities(patient, 50));

        return response;
    }
    @Override
    public Map<String, Object> getNotificationPreferencesByUsername(String username) {
        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("preferences", notificationPreferencesServiceCore.getNotificationPreferences(patient));
        return response;
    }

    @Override
    public Map<String, Object> updateNotificationPreferencesByUsername(String username, NotificationPreferencesDTO preferences) {
        Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Notification preferences updated successfully");
        response.put("preferences", notificationPreferencesServiceCore.updateNotificationPreferences(patient, preferences));
        return response;
    }

    @Override
    public Map<String, Object> getPrivacySettingsByUsername(String username) {
        return Map.of();
    }

    @Override
    public Map<String, Object> updatePrivacySettingsByUsername(String username, PrivacySettingsDTO settings) {
        return Map.of();
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
        dto.setResultUrl(labResult.getResultUrl());
        return dto;
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

        // Lab Results are managed via dedicated endpoints (upload/delete)
        // We do NOT update them here to avoid overwriting or duplicating entries
        /*
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
        */
    }

}
