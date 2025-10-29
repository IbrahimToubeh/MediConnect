package com.MediConnect.EntryRelated.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountActivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @Column
    private String location;
    
    @Column
    private String device;
    
    @Column
    private String userAgent;
    
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Additional data stored as JSON string for flexibility
    @Column(columnDefinition = "TEXT")
    private String additionalData;
    
    public enum ActivityType {
        LOGIN("Login"),
        LOGOUT("Logout"),
        PASSWORD_CHANGE("Password Change"),
        TWO_FA_ENABLED("2FA Enabled"),
        TWO_FA_DISABLED("2FA Disabled"),
        PROFILE_UPDATE("Profile Update"),
        ACCOUNT_CREATED("Account Created"),
        EMAIL_CHANGE("Email Change"),
        PHONE_CHANGE("Phone Change"),
        SECURITY_SETTINGS_CHANGE("Security Settings Change");
        
        private final String displayName;
        
        ActivityType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
