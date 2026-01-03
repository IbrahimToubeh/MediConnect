package com.MediConnect.EntryRelated.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreferences {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;
    
    // Email notifications
    @Column(nullable = false)
    private Boolean emailNotifications = true;
    
    // Push notifications
    @Column(nullable = false)
    private Boolean pushNotifications = true;
    
    // Social media notifications
    @Column(nullable = false)
    private Boolean postLikes = true;
    
    @Column(nullable = false)
    private Boolean postComments = true;
    
    @Column(nullable = false)
    private Boolean commentLikes = true;
    
    @Column(nullable = false)
    private Boolean commentReplies = true;
    
    // Healthcare notifications
    @Column(nullable = false)
    private Boolean appointmentReminders = true;
    
    @Column(nullable = false)
    private Boolean prescriptionUpdates = true;
    
    @Column(nullable = false)
    private Boolean labResults = true;
    
    @Column(nullable = false)
    private Boolean medicationReminders = true;
    
    // Security notifications
    @Column(nullable = false)
    private Boolean securityAlerts = true;
    
    @Column(nullable = false)
    private Boolean loginAlerts = true;
    
    @Column(nullable = false)
    private Boolean passwordChangeAlerts = true;
    
    // System notifications
    @Column(nullable = false)
    private Boolean systemUpdates = true;
    
    @Column(nullable = false)
    private Boolean maintenanceAlerts = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Method to check if a specific notification type is enabled
    public boolean isNotificationEnabled(String notificationType) {
        return switch (notificationType.toLowerCase()) {
            case "email" -> emailNotifications;
            case "push" -> pushNotifications;
            case "post_likes", "postlikes" -> postLikes;
            case "post_comments", "postcomments" -> postComments;
            case "comment_likes", "commentlikes" -> commentLikes;
            case "comment_replies", "commentreplies" -> commentReplies;
            case "appointment_reminders", "appointmentreminders" -> appointmentReminders;
            case "prescription_updates", "prescriptionupdates" -> prescriptionUpdates;
            case "lab_results", "labresults" -> labResults;
            case "medication_reminders", "medicationreminders" -> medicationReminders;
            case "security_alerts", "securityalerts" -> securityAlerts;
            case "login_alerts", "loginalerts" -> loginAlerts;
            case "password_change_alerts", "passwordchangealerts" -> passwordChangeAlerts;
            case "system_updates", "systemupdates" -> systemUpdates;
            case "maintenance_alerts", "maintenancealerts" -> maintenanceAlerts;
            default -> true; // Default to enabled for unknown types
        };
    }
}
