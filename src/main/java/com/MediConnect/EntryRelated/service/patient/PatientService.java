package com.MediConnect.EntryRelated.service.patient;

import com.MediConnect.EntryRelated.dto.ChangePasswordRequestDTO;
import com.MediConnect.EntryRelated.dto.NotificationPreferencesDTO;
import com.MediConnect.EntryRelated.dto.PrivacySettingsDTO;
import com.MediConnect.EntryRelated.dto.patient.LoginPatientRequestDTO;
import com.MediConnect.EntryRelated.dto.patient.SignupPatientRequestDTO;
import com.MediConnect.EntryRelated.dto.patient.UpdatePatientProfileRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component
public interface PatientService {
    String register(SignupPatientRequestDTO signupPatientRequestDTO);
    Map<String, Object> login(LoginPatientRequestDTO patientInfo, HttpServletRequest request);
    Map<String, Object> verifyLoginOTP(Map<String, String> request, HttpServletRequest httpRequest);
    Map<String, Object> getProfile(String token);
    Map<String, Object> getLabResults(String token);
    byte[] getLabResultImage(Long labResultId);
    Map<String, String> uploadLabResult(Long patientId, String description, MultipartFile imageFile) throws IOException;
    Map<String, Object> updateProfile(String username, UpdatePatientProfileRequestDTO updateRequest);
    Map<String, String> changePassword(String username, ChangePasswordRequestDTO changePasswordRequest);
    Map<String, String> enableTwoFactor(String username);
    Map<String, String> disableTwoFactor(String username);
    Map<String, Object> getTwoFactorStatus(String username);
    Map<String, Object> getActivity(String username);

    Map<String, Object> getNotificationPreferencesByUsername(String username);
    Map<String, Object> updateNotificationPreferencesByUsername(String username, NotificationPreferencesDTO preferences);
    Map<String, Object> getPrivacySettingsByUsername(String username);
    Map<String, Object> updatePrivacySettingsByUsername(String username, PrivacySettingsDTO settings);

}
