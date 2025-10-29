package com.MediConnect.EntryRelated.repository;

import com.MediConnect.EntryRelated.entities.UserPrivacySettings;
import com.MediConnect.EntryRelated.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPrivacySettingsRepository extends JpaRepository<UserPrivacySettings, Long> {
    
    Optional<UserPrivacySettings> findByUser(Users user);
    
    Optional<UserPrivacySettings> findByUserId(Long userId);
    
    boolean existsByUser(Users user);
}
