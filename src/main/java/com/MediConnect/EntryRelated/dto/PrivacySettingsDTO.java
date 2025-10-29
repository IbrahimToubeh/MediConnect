package com.MediConnect.EntryRelated.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettingsDTO {
    
    private String profileVisibility = "public"; // "public" or "private"
    private Boolean showEmail = false;
    private Boolean showPhone = false;
    private Boolean showAddress = false;
    private Boolean showMedicalHistory = false;
}
