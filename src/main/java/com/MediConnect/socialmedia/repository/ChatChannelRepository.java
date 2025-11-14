package com.MediConnect.socialmedia.repository;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.socialmedia.entity.ChatChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChatChannel entities
 * 
 * Provides database operations for chat channels.
 */
@Repository
public interface ChatChannelRepository extends JpaRepository<ChatChannel, Long> {
    
    /**
     * Find the chat channel between a specific patient and doctor
     * Returns empty if no channel exists
     * 
     * This is the main method used to check if a chat channel already exists
     * before creating a new one (ensures only one channel per patient-doctor pair)
     */
    Optional<ChatChannel> findByPatientAndDoctor(Patient patient, HealthcareProvider doctor);
    
    /**
     * Get all chat channels for a patient
     * Ordered by most recent activity (newest first)
     * Returns only one channel per doctor (handles duplicates)
     */
    List<ChatChannel> findByPatientOrderByLastActivityAtDesc(Patient patient);
    
    /**
     * Get all chat channels for a doctor
     * Ordered by most recent activity (newest first)
     * Returns only one channel per patient (handles duplicates)
     */
    List<ChatChannel> findByDoctorOrderByLastActivityAtDesc(HealthcareProvider doctor);
    
    /**
     * Find all duplicate channels for a patient-doctor pair
     * Used for cleanup - returns all channels between patient and doctor
     */
    List<ChatChannel> findByPatientAndDoctorOrderByLastActivityAtDesc(Patient patient, HealthcareProvider doctor);
}

