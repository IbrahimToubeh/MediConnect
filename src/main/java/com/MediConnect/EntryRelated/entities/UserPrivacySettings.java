package com.MediConnect.EntryRelated.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_privacy_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPrivacySettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;
    
    @Column(nullable = false)
    private String profileVisibility = "public"; // "public" or "private"
    
    @Column(nullable = false)
    private Boolean showEmail = false;
    
    @Column(nullable = false)
    private Boolean showPhone = false;
    
    @Column(nullable = false)
    private Boolean showAddress = false;
    
    @Column(nullable = false)
    private Boolean showMedicalHistory = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
