package com.MediConnect.EntryRelated.repository;

import com.MediConnect.EntryRelated.entities.UserNotificationPreferences;
import com.MediConnect.EntryRelated.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserNotificationPreferencesRepository extends JpaRepository<UserNotificationPreferences, Long> {
    
    Optional<UserNotificationPreferences> findByUser(Users user);
    
    Optional<UserNotificationPreferences> findByUserId(Long userId);
    
    boolean existsByUser(Users user);
}
