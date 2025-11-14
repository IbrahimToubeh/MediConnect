package com.MediConnect.EntryRelated.repository;

import com.MediConnect.Entities.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MediConnect.Entities.AppointmentStatus;
import java.util.Date;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Integer> {
    List<AppointmentEntity> findByPatientId(Long patientId);
    List<AppointmentEntity> findByHealthcareProviderId(Long providerId);
    
    /**
     * Find confirmed appointments that need a 24-hour reminder
     * Appointments that are:
     * - Status is CONFIRMED
     * - Appointment time is between 24 and 36 hours from now (12-hour window since we check every 12 hours)
     * - reminder_24h_sent is false
     */
    @Query("SELECT a FROM AppointmentEntity a WHERE a.status = :status " +
           "AND a.appointmentDateTime >= :startTime " +
           "AND a.appointmentDateTime <= :endTime " +
           "AND a.reminder24hSent = false")
    List<AppointmentEntity> findAppointmentsNeeding24hReminder(
        @Param("status") AppointmentStatus status,
        @Param("startTime") Date startTime,
        @Param("endTime") Date endTime
    );
    
    // Temporarily commented out to test if this is causing the startup issue
    // Uncomment after confirming the application starts successfully
    /*
    @Query(value = "SELECT * FROM appointment_entity " +
           "WHERE provider_id = :doctorId " +
           "AND appointment_date_time >= :startOfDay " +
           "AND appointment_date_time < :endOfDay " +
           "AND status = :status", nativeQuery = true)
    List<AppointmentEntity> findConfirmedAppointmentsByDoctorAndDate(
        @Param("doctorId") Long doctorId,
        @Param("startOfDay") Timestamp startOfDay,
        @Param("endOfDay") Timestamp endOfDay,
        @Param("status") String status
    );
    */
}

