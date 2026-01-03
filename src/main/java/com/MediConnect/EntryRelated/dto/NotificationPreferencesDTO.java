package com.MediConnect.EntryRelated.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesDTO {
    
    // Email notifications
    private Boolean emailNotifications = true;
    
    // Push notifications
    private Boolean pushNotifications = true;
    
    // Social media notifications
    private Boolean postLikes = true;
    private Boolean postComments = true;
    private Boolean commentLikes = true;
    private Boolean commentReplies = true;
    
    // Healthcare notifications
    private Boolean appointmentReminders = true;
    private Boolean prescriptionUpdates = true;
    private Boolean labResults = true;
    private Boolean medicationReminders = true;
    
    // Security notifications
    private Boolean securityAlerts = true;
    private Boolean loginAlerts = true;
    private Boolean passwordChangeAlerts = true;
    
    // System notifications
    private Boolean systemUpdates = true;
    private Boolean maintenanceAlerts = true;
}
