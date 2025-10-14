package com.MediConnect.EntryRelated.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;
    
    @Column(nullable = false)
    private String sessionToken;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @Column(nullable = false)
    private String userAgent;
    
    @Column
    private String location;
    
    @Column
    private String device;
    
    @Column
    private String browser;
    
    @Column(nullable = false)
    private LocalDateTime loginTime;
    
    @Column
    private LocalDateTime logoutTime;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime lastActivityTime;
    
    // Method to mark session as inactive
    public void logout() {
        this.isActive = false;
        this.logoutTime = LocalDateTime.now();
    }
    
    // Method to update last activity
    public void updateActivity() {
        this.lastActivityTime = LocalDateTime.now();
    }
}
