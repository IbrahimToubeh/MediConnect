package com.MediConnect.EntryRelated.service;
// t
import com.MediConnect.EntryRelated.dto.PrivacySettingsDTO;
import com.MediConnect.EntryRelated.entities.UserPrivacySettings;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.repository.UserPrivacySettingsRepository;
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
public class PrivacySettingsService {

    private final UserPrivacySettingsRepository privacySettingsRepository;
    private final UserRepo userRepository;

    /**
     * Get privacy settings for a user, creating default settings if none exist
     */
    @Transactional
    public PrivacySettingsDTO getPrivacySettings(Users user) {
        UserPrivacySettings settings = privacySettingsRepository.findByUser(user)
            .orElseGet(() -> createDefaultSettings(user));

        return convertToDTO(settings);
    }

    /**
     * Update privacy settings for a user
     */
    @Transactional
    public PrivacySettingsDTO updatePrivacySettings(Users user, PrivacySettingsDTO dto) {
        UserPrivacySettings settings = privacySettingsRepository.findByUser(user)
            .orElseGet(() -> createDefaultSettings(user));

        // Update all fields
        settings.setProfileVisibility(dto.getProfileVisibility());
        settings.setShowEmail(dto.getShowEmail());
        settings.setShowPhone(dto.getShowPhone());
        settings.setShowAddress(dto.getShowAddress());
        settings.setShowMedicalHistory(dto.getShowMedicalHistory());

        UserPrivacySettings saved = privacySettingsRepository.save(settings);
        log.info("Updated privacy settings for user: {}", user.getUsername());

        return convertToDTO(saved);
    }

    /**
     * Check if a user's profile is public
     */
    public boolean isProfilePublic(Users user) {
        UserPrivacySettings settings = privacySettingsRepository.findByUser(user)
            .orElseGet(() -> createDefaultSettings(user));

        return "public".equalsIgnoreCase(settings.getProfileVisibility());
    }

    /**
     * Check if a user's profile is public by user ID
     */
    public boolean isProfilePublicByUserId(Long userId) {
        UserPrivacySettings settings = privacySettingsRepository.findByUserId(userId)
            .orElse(null);

        // Default to public if no settings exist
        if (settings == null) {
            return true;
        }

        return "public".equalsIgnoreCase(settings.getProfileVisibility());
    }

    /**
     * Create default privacy settings for a new user
     */
    private UserPrivacySettings createDefaultSettings(Users user) {
        UserPrivacySettings settings = new UserPrivacySettings();
        settings.setUser(user);
        // All fields are already set to default values in the entity

        UserPrivacySettings saved = privacySettingsRepository.save(settings);
        log.info("Created default privacy settings for user: {}", user.getUsername());
        return saved;
    }

    /**
     * Convert entity to DTO
     */
    private PrivacySettingsDTO convertToDTO(UserPrivacySettings settings) {
        PrivacySettingsDTO dto = new PrivacySettingsDTO();
        dto.setProfileVisibility(settings.getProfileVisibility());
        dto.setShowEmail(settings.getShowEmail());
        dto.setShowPhone(settings.getShowPhone());
        dto.setShowAddress(settings.getShowAddress());
        dto.setShowMedicalHistory(settings.getShowMedicalHistory());

        return dto;
    }
    public Map<String, Object> getPrivacySettingsByUsername(String username) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("settings", getPrivacySettings(user));
        return response;
    }

    public Map<String, Object> updatePrivacySettingsByUsername(String username, PrivacySettingsDTO dto) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Privacy settings updated successfully");
        response.put("settings", updatePrivacySettings(user, dto));
        return response;
    }
}
