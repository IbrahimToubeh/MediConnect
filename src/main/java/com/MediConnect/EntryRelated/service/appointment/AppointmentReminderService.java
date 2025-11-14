package com.MediConnect.EntryRelated.service.appointment;

import com.MediConnect.Entities.AppointmentEntity;
import com.MediConnect.Entities.AppointmentStatus;
import com.MediConnect.EntryRelated.repository.AppointmentRepository;
import com.MediConnect.socialmedia.entity.NotificationType;
import com.MediConnect.socialmedia.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Appointment Reminder Service
 * 
 * Scheduled service that automatically sends reminder notifications for upcoming appointments:
 * - 24 hours before appointment: Both patient and doctor receive a reminder
 * 
 * Runs every 12 hours to check for appointments that need reminders.
 * Only sends reminders for CONFIRMED appointments.
 * Each reminder is sent only once per appointment (tracked by reminder_24h_sent flag).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderService {
    
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;
    
    /**
     * Scheduled task that runs every 12 hours
     * Checks for appointments that need 24-hour reminders
     */
    @Scheduled(fixedRate = 43200000) // 43200000 milliseconds = 12 hours
    @Transactional
    public void send24HourReminders() {
        try {
            log.info("=== Checking for appointments needing 24-hour reminders ===");
            
            // Calculate time window: appointments between 24 and 36 hours from now
            // This gives us a 12-hour window to catch appointments (since we check every 12 hours)
            Calendar cal = Calendar.getInstance();
            
            // 24 hours from now
            cal.add(Calendar.HOUR_OF_DAY, 24);
            Date startTime = cal.getTime();
            
            // 36 hours from now (12-hour window)
            cal.add(Calendar.HOUR_OF_DAY, 12);
            Date endTime = cal.getTime();
            
            log.info("Looking for appointments between {} and {}", startTime, endTime);
            
            // Find confirmed appointments that need 24-hour reminder
            List<AppointmentEntity> appointments = appointmentRepository.findAppointmentsNeeding24hReminder(
                AppointmentStatus.CONFIRMED,
                startTime,
                endTime
            );
            
            log.info("Found {} appointments needing 24-hour reminders", appointments.size());
            
            // Send reminders for each appointment
            for (AppointmentEntity appointment : appointments) {
                try {
                    // Send notification to both patient and doctor
                    notificationService.createAppointmentReminderNotification(
                        appointment.getPatient(),
                        appointment.getHealthcareProvider(),
                        NotificationType.APPOINTMENT_REMINDER_24H,
                        (long) appointment.getId(),
                        appointment.getAppointmentDateTime()
                    );
                    
                    // Mark reminder as sent
                    appointment.setReminder24hSent(true);
                    appointmentRepository.save(appointment);
                    
                    log.info("Sent 24-hour reminder for appointment ID: {} (Patient: {}, Doctor: {})", 
                        appointment.getId(),
                        appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName(),
                        appointment.getHealthcareProvider().getFirstName() + " " + appointment.getHealthcareProvider().getLastName());
                } catch (Exception e) {
                    log.error("Failed to send 24-hour reminder for appointment ID: {}", appointment.getId(), e);
                    // Continue with next appointment even if this one fails
                }
            }
            
            log.info("=== Finished checking 24-hour reminders ===");
        } catch (Exception e) {
            log.error("Error in scheduled 24-hour reminder task", e);
        }
    }
}

