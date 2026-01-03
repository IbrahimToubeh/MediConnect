package com.MediConnect.EntryRelated.service;

import com.MediConnect.EntryRelated.dto.NotificationPreferencesDTO;
import com.MediConnect.EntryRelated.entities.UserNotificationPreferences;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.repository.UserNotificationPreferencesRepository;
import com.MediConnect.Repos.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferencesService {
    
    private final UserNotificationPreferencesRepository preferencesRepository;
    private final UserRepo userRepository;

    /**
     * Get notification preferences for a user, creating default preferences if none exist
     */
    @Transactional
    public NotificationPreferencesDTO getNotificationPreferences(Users user) {
        UserNotificationPreferences preferences = preferencesRepository.findByUser(user)
            .orElseGet(() -> createDefaultPreferences(user));
        
        return convertToDTO(preferences);
    }
    
    /**
     * Update notification preferences for a user
     */
    @Transactional
    public NotificationPreferencesDTO updateNotificationPreferences(Users user, NotificationPreferencesDTO dto) {
        UserNotificationPreferences preferences = preferencesRepository.findByUser(user)
            .orElseGet(() -> createDefaultPreferences(user));
        
        // Update all fields
        preferences.setEmailNotifications(dto.getEmailNotifications());
        preferences.setPushNotifications(dto.getPushNotifications());
        preferences.setPostLikes(dto.getPostLikes());
        preferences.setPostComments(dto.getPostComments());
        preferences.setCommentLikes(dto.getCommentLikes());
        preferences.setCommentReplies(dto.getCommentReplies());
        preferences.setAppointmentReminders(dto.getAppointmentReminders());
        preferences.setPrescriptionUpdates(dto.getPrescriptionUpdates());
        preferences.setLabResults(dto.getLabResults());
        preferences.setMedicationReminders(dto.getMedicationReminders());
        preferences.setSecurityAlerts(dto.getSecurityAlerts());
        preferences.setLoginAlerts(dto.getLoginAlerts());
        preferences.setPasswordChangeAlerts(dto.getPasswordChangeAlerts());
        preferences.setSystemUpdates(dto.getSystemUpdates());
        preferences.setMaintenanceAlerts(dto.getMaintenanceAlerts());
        
        UserNotificationPreferences saved = preferencesRepository.save(preferences);
        log.info("Updated notification preferences for user: {}", user.getUsername());
        
        return convertToDTO(saved);
    }
    
    /**
     * Check if a specific notification type is enabled for a user
     */
    public boolean isNotificationEnabled(Users user, String notificationType) {
        UserNotificationPreferences preferences = preferencesRepository.findByUser(user)
            .orElseGet(() -> createDefaultPreferences(user));
        
        return preferences.isNotificationEnabled(notificationType);
    }
    
    /**
     * Create default notification preferences for a new user
     */
    private UserNotificationPreferences createDefaultPreferences(Users user) {
        UserNotificationPreferences preferences = new UserNotificationPreferences();
        preferences.setUser(user);
        // All fields are already set to true by default in the entity
        
        UserNotificationPreferences saved = preferencesRepository.save(preferences);
        log.info("Created default notification preferences for user: {}", user.getUsername());
        return saved;
    }
    
    /**
     * Convert entity to DTO
     */
    private NotificationPreferencesDTO convertToDTO(UserNotificationPreferences preferences) {
        NotificationPreferencesDTO dto = new NotificationPreferencesDTO();
        dto.setEmailNotifications(preferences.getEmailNotifications());
        dto.setPushNotifications(preferences.getPushNotifications());
        dto.setPostLikes(preferences.getPostLikes());
        dto.setPostComments(preferences.getPostComments());
        dto.setCommentLikes(preferences.getCommentLikes());
        dto.setCommentReplies(preferences.getCommentReplies());
        dto.setAppointmentReminders(preferences.getAppointmentReminders());
        dto.setPrescriptionUpdates(preferences.getPrescriptionUpdates());
        dto.setLabResults(preferences.getLabResults());
        dto.setMedicationReminders(preferences.getMedicationReminders());
        dto.setSecurityAlerts(preferences.getSecurityAlerts());
        dto.setLoginAlerts(preferences.getLoginAlerts());
        dto.setPasswordChangeAlerts(preferences.getPasswordChangeAlerts());
        dto.setSystemUpdates(preferences.getSystemUpdates());
        dto.setMaintenanceAlerts(preferences.getMaintenanceAlerts());
        
        return dto;
    }
    public Map<String, Object> getNotificationPreferencesByUsername(String username) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("preferences", getNotificationPreferences(user));
        return response;
    }

    public Map<String, Object> updateNotificationPreferencesByUsername(String username, NotificationPreferencesDTO dto) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Notification preferences updated successfully");
        response.put("preferences", updateNotificationPreferences(user, dto));
        return response;
    }
}
