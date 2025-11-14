package com.MediConnect.EntryRelated.service.appointment.impl;

import com.MediConnect.Entities.AppointmentEntity;
import com.MediConnect.Entities.AppointmentStatus;
import com.MediConnect.Entities.AppointmentType;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.repository.AppointmentRepository;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.service.appointment.AppointmentService;
import com.MediConnect.config.JWTService;
import com.MediConnect.socialmedia.service.NotificationService;
import com.MediConnect.socialmedia.entity.NotificationType;
import com.MediConnect.socialmedia.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Appointment Service Implementation
 * 
 * Handles appointment booking, retrieval, and status updates with integrated notification system.
 * 
 * NOTIFICATION WORKFLOW:
 * 1. Patient books → Doctor gets APPOINTMENT_REQUESTED notification
 * 2. Doctor confirms/cancels/reschedules → Patient gets status change notification
 * 3. Patient responds to reschedule → Doctor gets response notification
 * 
 * MEDICAL RECORDS SHARING:
 * - Controlled by shareMedicalRecords field set during booking
 * - If true: Doctor can see allergies, conditions, surgeries
 * - If false: Medical records hidden for privacy
 * - Insurance info is ALWAYS visible to doctors (billing purposes)
 */
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepo patientRepo;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final JWTService jwtService;
    private final NotificationService notificationService; // Used for automatic appointment notifications
    private final ChatService chatService; // Used to create chat channels when appointments are confirmed

    @Override
    public Map<String, Object> bookAppointment(String token, Map<String, Object> request) {
        try {
            // Extract token
            String jwtToken = token != null && token.startsWith("Bearer ") 
                ? token.substring(7) 
                : token;

            // Get patient from token
            String username = jwtService.extractUserName(jwtToken);
            Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

            // Get doctor
            Long doctorId = Long.parseLong(request.get("doctorId").toString());
            HealthcareProvider doctor = healthcareProviderRepo.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

            // Parse appointment date-time
            if (request.get("appointmentDateTime") == null) {
                throw new RuntimeException("Appointment date and time is required");
            }
            String dateTimeStr = request.get("appointmentDateTime").toString();
            Date appointmentDateTime;
            try {
                // Parse ISO date-time string
                String cleanedDateTime = dateTimeStr.replace("Z", "").replace("z", "");
                LocalDateTime localDateTime = LocalDateTime.parse(cleanedDateTime);
                appointmentDateTime = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
            } catch (Exception e) {
                try {
                    // Try alternative parsing with SimpleDateFormat
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    appointmentDateTime = sdf.parse(dateTimeStr);
                } catch (Exception e2) {
                    // Last resort: try simple date format
                    try {
                        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        appointmentDateTime = sdf2.parse(dateTimeStr);
                    } catch (Exception e3) {
                        throw new RuntimeException("Invalid appointment date format: " + dateTimeStr, e3);
                    }
                }
            }

            // Validate date is in future
            if (appointmentDateTime.before(new Date())) {
                throw new RuntimeException("Appointment date must be in the future");
            }

            // Create appointment
            AppointmentEntity appointment = new AppointmentEntity();
            appointment.setPatient(patient);
            appointment.setHealthcareProvider(doctor);
            appointment.setAppointmentDateTime(appointmentDateTime);
            appointment.setStatus(AppointmentStatus.PENDING);
            appointment.setType(AppointmentType.CONSULTATION); // Default type
            appointment.setReason(request.get("description") != null ? request.get("description").toString() : "");
            
            // MEDICAL RECORDS SHARING FEATURE:
            // The shareMedicalRecords field controls whether the doctor can see the patient's medical history
            // - If true: Doctor can see allergies, medical conditions, and previous surgeries
            // - If false: Doctor cannot see medical records (privacy protection)
            // Defaults to false for privacy if not specified
            Boolean shareMedicalRecords = false;
            if (request.get("shareMedicalRecords") != null) {
                Object shareValue = request.get("shareMedicalRecords");
                if (shareValue instanceof Boolean) {
                    shareMedicalRecords = (Boolean) shareValue;
                } else if (shareValue instanceof String) {
                    shareMedicalRecords = Boolean.parseBoolean((String) shareValue);
                }
            }
            appointment.setShareMedicalRecords(shareMedicalRecords);

            // VIDEO CALL FEATURE:
            // Video call appointments are only available for psychiatry doctors
            // If doctor is a psychiatrist and patient requested video call, set the flag
            Boolean isVideoCall = false;
            if (request.get("isVideoCall") != null) {
                Object videoCallValue = request.get("isVideoCall");
                if (videoCallValue instanceof Boolean) {
                    isVideoCall = (Boolean) videoCallValue;
                } else if (videoCallValue instanceof String) {
                    isVideoCall = Boolean.parseBoolean((String) videoCallValue);
                }
            }
            
            // Validate: Video call only allowed for psychiatry doctors
            boolean isPsychiatrist = doctor.getSpecializations() != null && 
                doctor.getSpecializations().stream()
                    .anyMatch(spec -> spec != null && spec.name().equals("PSYCHIATRY"));
            
            if (isVideoCall && !isPsychiatrist) {
                throw new RuntimeException("Video call appointments are only available for psychiatry doctors");
            }
            
            appointment.setIsVideoCall(isVideoCall);

            appointment = appointmentRepository.save(appointment);

            // NOTIFICATION: Automatically notify the doctor when a patient books an appointment
            // This creates an APPOINTMENT_REQUESTED notification that appears in the doctor's notification list
            // Notification is wrapped in try-catch to ensure appointment booking succeeds even if notification fails
            try {
                notificationService.createAppointmentRequestedNotification(patient, doctor, (long) appointment.getId());
            } catch (Exception e) {
                // Log but don't fail the appointment booking艳notification fails
                System.err.println("Failed to create appointment notification: " + e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Appointment booked successfully");
            response.put("appointmentId", appointment.getId());
            return response;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }

    @Override
    public Map<String, Object> getPatientAppointments(String token) {
        try {
            String jwtToken = token != null && token.startsWith("Bearer ") 
                ? token.substring(7) 
                : token;

            String username = jwtService.extractUserName(jwtToken);
            Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

            List<AppointmentEntity> appointments = appointmentRepository.findByPatientId(patient.getId());
            List<Map<String, Object>> appointmentList = new ArrayList<>();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

            for (AppointmentEntity apt : appointments) {
                Map<String, Object> aptMap = new HashMap<>();
                aptMap.put("id", apt.getId());
                aptMap.put("patientId", apt.getPatient().getId());
                aptMap.put("patientName", apt.getPatient().getFirstName() + " " + apt.getPatient().getLastName());
                aptMap.put("patientEmail", apt.getPatient().getEmail());
                aptMap.put("patientPhone", apt.getPatient().getPhoneNumber());

                aptMap.put("doctorId", apt.getHealthcareProvider().getId());
                aptMap.put("doctorName", "Dr. " + apt.getHealthcareProvider().getFirstName() + " " + apt.getHealthcareProvider().getLastName());
                aptMap.put("doctorSpecialty", apt.getHealthcareProvider().getSpecializations() != null && !apt.getHealthcareProvider().getSpecializations().isEmpty()
                    ? apt.getHealthcareProvider().getSpecializations().get(0).name() : "");
                aptMap.put("doctorEmail", apt.getHealthcareProvider().getEmail());
                aptMap.put("doctorPhone", apt.getHealthcareProvider().getPhoneNumber());
                aptMap.put("doctorProfilePicture", apt.getHealthcareProvider().getProfilePicture()); // Include doctor's profile picture

                if (apt.getAppointmentDateTime() != null) {
                    aptMap.put("appointmentDateTime", apt.getAppointmentDateTime().toInstant().toString());
                    aptMap.put("date", dateFormat.format(apt.getAppointmentDateTime()));
                    aptMap.put("time", timeFormat.format(apt.getAppointmentDateTime()));
                }

                aptMap.put("description", apt.getReason() != null ? apt.getReason() : "");
                aptMap.put("shareMedicalRecords", apt.getShareMedicalRecords() != null ? apt.getShareMedicalRecords() : false);
                aptMap.put("isVideoCall", apt.getIsVideoCall() != null ? apt.getIsVideoCall() : false);
                aptMap.put("isCallActive", apt.getIsCallActive() != null ? apt.getIsCallActive() : false);
                // expose insurance details to doctor
                aptMap.put("insuranceProvider", apt.getPatient().getInsuranceProvider());
                aptMap.put("insuranceNumber", apt.getPatient().getInsuranceNumber());
                // include minimal medical record summary if sharing is enabled
                if (Boolean.TRUE.equals(apt.getShareMedicalRecords())) {
                    Map<String, Object> med = new HashMap<>();
                    med.put("allergies", apt.getPatient().getAllergies());
                    med.put("medicalConditions", apt.getPatient().getMedicalConditions());
                    med.put("previousSurgeries", apt.getPatient().getPreviousSurgeries());
                    aptMap.put("medicalRecords", med);
                }
                aptMap.put("status", apt.getStatus() != null ? apt.getStatus().name().toLowerCase() : "pending");
                aptMap.put("createdAt", apt.getCreatedAt() != null ? apt.getCreatedAt().toInstant().toString() : new Date().toInstant().toString());
                
                if (apt.getNotes() != null && !apt.getNotes().isEmpty()) {
                    aptMap.put("doctorNotes", apt.getNotes());
                }

                appointmentList.add(aptMap);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", appointmentList);
            return response;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }

    @Override
    public Map<String, Object> getDoctorAppointments(String token) {
        try {
            String jwtToken = token != null && token.startsWith("Bearer ") 
                ? token.substring(7) 
                : token;

            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider doctor = healthcareProviderRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

            List<AppointmentEntity> appointments = appointmentRepository.findByHealthcareProviderId(doctor.getId());
            List<Map<String, Object>> appointmentList = new ArrayList<>();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

            for (AppointmentEntity apt : appointments) {
                Map<String, Object> aptMap = new HashMap<>();
                aptMap.put("id", apt.getId());
                aptMap.put("patientId", apt.getPatient().getId());
                aptMap.put("patientName", apt.getPatient().getFirstName() + " " + apt.getPatient().getLastName());
                aptMap.put("patientEmail", apt.getPatient().getEmail());
                aptMap.put("patientPhone", apt.getPatient().getPhoneNumber());

                aptMap.put("doctorId", apt.getHealthcareProvider().getId());
                aptMap.put("doctorName", "Dr. " + apt.getHealthcareProvider().getFirstName() + " " + apt.getHealthcareProvider().getLastName());
                aptMap.put("doctorSpecialty", apt.getHealthcareProvider().getSpecializations() != null && !apt.getHealthcareProvider().getSpecializations().isEmpty()
                    ? apt.getHealthcareProvider().getSpecializations().get(0).name() : "");
                aptMap.put("doctorEmail", apt.getHealthcareProvider().getEmail());
                aptMap.put("doctorPhone", apt.getHealthcareProvider().getPhoneNumber());
                aptMap.put("doctorProfilePicture", apt.getHealthcareProvider().getProfilePicture()); // Include doctor's profile picture

                if (apt.getAppointmentDateTime() != null) {
                    aptMap.put("appointmentDateTime", apt.getAppointmentDateTime().toInstant().toString());
                    aptMap.put("date", dateFormat.format(apt.getAppointmentDateTime()));
                    aptMap.put("time", timeFormat.format(apt.getAppointmentDateTime()));
                }

                aptMap.put("description", apt.getReason() != null ? apt.getReason() : "");
                aptMap.put("shareMedicalRecords", apt.getShareMedicalRecords() != null ? apt.getShareMedicalRecords() : false);
                aptMap.put("isVideoCall", apt.getIsVideoCall() != null ? apt.getIsVideoCall() : false);
                aptMap.put("isCallActive", apt.getIsCallActive() != null ? apt.getIsCallActive() : false);
                // expose insurance details to doctor
                aptMap.put("insuranceProvider", apt.getPatient().getInsuranceProvider());
                aptMap.put("insuranceNumber", apt.getPatient().getInsuranceNumber());
                // include minimal medical record summary if sharing is enabled
                if (Boolean.TRUE.equals(apt.getShareMedicalRecords())) {
                    Map<String, Object> med = new HashMap<>();
                    med.put("allergies", apt.getPatient().getAllergies());
                    med.put("medicalConditions", apt.getPatient().getMedicalConditions());
                    med.put("previousSurgeries", apt.getPatient().getPreviousSurgeries());
                    aptMap.put("medicalRecords", med);
                }
                aptMap.put("status", apt.getStatus() != null ? apt.getStatus().name().toLowerCase() : "pending");
                aptMap.put("createdAt", apt.getCreatedAt() != null ? apt.getCreatedAt().toInstant().toString() : new Date().toInstant().toString());


                if (apt.getNotes() != null && !apt.getNotes().isEmpty()) {
                    aptMap.put("doctorNotes", apt.getNotes());
                }

                appointmentList.add(aptMap);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", appointmentList);
            return response;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }

    @Override
    public Map<String, Object> updateAppointmentStatus(String token, Integer appointmentId, String status, String note, String newDateTime) {
        try {
            String jwtToken = token != null && token.startsWith("Bearer ")
                ? token.substring(7)
                : token;

            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider doctor = healthcareProviderRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

            AppointmentEntity apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

            if (!apt.getHealthcareProvider().getId().equals(doctor.getId())) {
                throw new RuntimeException("You are not allowed to modify this appointment");
            }

            // Update appointment status (allowed values: CONFIRMED, CANCELLED, RESCHEDULED)
            AppointmentStatus newStatus = AppointmentStatus.valueOf(status.toUpperCase());
            apt.setStatus(newStatus);
            
            // RESCHEDULING LOGIC: If doctor is rescheduling, update the appointment date/time
            // The newDateTime parameter contains the new appointment time in ISO format
            // When status is RESCHEDULED, the patient will need to confirm the new time via respondToReschedule()
            if ("RESCHEDULED".equals(status) && newDateTime != null && !newDateTime.isEmpty()) {
                try {
                    // Parse the new date/time for the rescheduled appointment
                    String cleanedDateTime = newDateTime.replace("Z", "").replace("z", "");
                    LocalDateTime localDateTime = LocalDateTime.parse(cleanedDateTime);
                    Date appointmentDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                    apt.setAppointmentDateTime(appointmentDate);
                    
                    // Reset reminder flag when appointment is rescheduled
                    // This ensures reminders will be sent for the new appointment time
                    apt.setReminder24hSent(false);
                } catch (Exception e) {
                    try {
                        // Fallback parsing if ISO format fails
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        apt.setAppointmentDateTime(sdf.parse(newDateTime));
                        
                        // Reset reminder flag when appointment is rescheduled
                        apt.setReminder24hSent(false);
                    } catch (Exception e2) {
                        throw new RuntimeException("Invalid date format for rescheduling", e2);
                    }
                }
            }
            
            // Save optional doctor notes (e.g., cancellation reason, confirmation message)
            if (note != null && !note.isBlank()) {
                apt.setNotes(note);
            }

            // Save the appointment with updated status
            apt = appointmentRepository.save(apt);
            
            // Reload the appointment to ensure all relationships are loaded
            // Note: No explicit flush() needed - Spring will flush at transaction commit
            // The entity is already in the persistence context, so findById() will retrieve it from there
            // Note: AppointmentEntity.id is int, not Long
            apt = appointmentRepository.findById(apt.getId())
                .orElseThrow(() -> new RuntimeException("Appointment not found after save"));

            // CHAT CHANNEL CREATION: When appointment is confirmed, create a chat channel
            // This allows patient and doctor to communicate in real-time
            // Only ONE channel is created per patient-doctor pair (even with multiple appointments)
            // This works for ALL confirmed appointments, including video call appointments with psychiatry doctors
            if ("CONFIRMED".equalsIgnoreCase(status) || apt.getStatus() == AppointmentStatus.CONFIRMED) {
                try {
                    System.out.println("DEBUG: Creating chat channel for confirmed appointment ID: " + apt.getId());
                    System.out.println("DEBUG: Appointment status: " + apt.getStatus());
                    System.out.println("DEBUG: Patient ID: " + apt.getPatient().getId());
                    System.out.println("DEBUG: Doctor ID: " + apt.getHealthcareProvider().getId());
                    chatService.createChannelForConfirmedAppointment(apt);
                    System.out.println("SUCCESS: Chat channel created for confirmed appointment ID: " + apt.getId());
                } catch (Exception e) {
                    // Log but don't fail the appointment confirmation if chat creation fails
                    System.err.println("ERROR: Failed to create chat channel for appointment ID: " + apt.getId());
                    System.err.println("ERROR: Exception type: " + e.getClass().getSimpleName());
                    System.err.println("ERROR: Exception message: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("DEBUG: Skipping chat channel creation - Status is: " + apt.getStatus() + " (expected CONFIRMED)");
            }

            // NOTIFICATION: Automatically notify patient when doctor changes appointment status
            // This creates different notification types based on the action:
            // - CONFIRMED → APPOINTMENT_CONFIRMED notification (appointment is confirmed)
            // - CANCELLED → APPOINTMENT_CANCELLED notification (appointment is cancelled)
            // - RESCHEDULED → APPOINTMENT_RESCHEDULED notification (includes new date/time in message)
            try {
                NotificationType notificationType = null;
                String additionalInfo = null;
                
                // Map status to appropriate notification type
                if ("CONFIRMED".equals(status)) {
                    notificationType = NotificationType.APPOINTMENT_CONFIRMED;
                } else if ("CANCELLED".equals(status)) {
                    notificationType = NotificationType.APPOINTMENT_CANCELLED;
                } else if ("RESCHEDULED".equals(status)) {
                    notificationType = NotificationType.APPOINTMENT_RESCHEDULED;
                    // Include new date/time in notification message for better UX
                    if (apt.getAppointmentDateTime() != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
                        additionalInfo = "New time: " + dateFormat.format(apt.getAppointmentDateTime());
                    }
                }
                
                if (notificationType != null) {
                    notificationService.createAppointmentStatusNotification(
                        doctor, apt.getPatient(), notificationType, (long) apt.getId(), additionalInfo);
                }
            } catch (Exception e) {
                // Log but don't fail the update if notification fails
                System.err.println("Failed to create appointment status notification: " + e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Appointment updated");
            response.put("appointmentId", apt.getId());
            response.put("newStatus", apt.getStatus().name().toLowerCase());
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }

    @Override
    public Map<String, Object> respondToReschedule(String token, Integer appointmentId, String action) {
        try {
            String jwtToken = token != null && token.startsWith("Bearer ")
                ? token.substring(7)
                : token;

            String username = jwtService.extractUserName(jwtToken);
            Patient patient = patientRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

            AppointmentEntity apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

            if (!apt.getPatient().getId().equals(patient.getId())) {
                throw new RuntimeException("You are not allowed to modify this appointment");
            }

            // VALIDATION: Only allow patient to respond if appointment is in RESCHEDULED status
            // This ensures the workflow: Doctor reschedules → Status = RESCHEDULED → Patient responds
            if (!apt.getStatus().equals(AppointmentStatus.RESCHEDULED)) {
                throw new RuntimeException("Appointment is not in rescheduled status");
            }

            // Update status based on patient's response to reschedule:
            // - "confirm" → Patient accepts the new time, status becomes CONFIRMED
            // - "cancel" → Patient rejects the new time, appointment is CANCELLED
            if ("confirm".equalsIgnoreCase(action)) {
                apt.setStatus(AppointmentStatus.CONFIRMED);
            } else if ("cancel".equalsIgnoreCase(action)) {
                apt.setStatus(AppointmentStatus.CANCELLED);
            } else {
                throw new RuntimeException("Invalid action. Use 'confirm' or 'cancel'");
            }

            appointmentRepository.save(apt);

            // NOTIFICATION: Automatically notify doctor when patient responds to reschedule request
            // This completes the rescheduling workflow notification cycle:
            // 1. Doctor reschedules → Patient gets APPOINTMENT_RESCHEDULED notification
            // 2. Patient responds → Doctor gets notification about the response:
            //    - APPOINTMENT_RESCHEDULE_CONFIRMED (patient accepted new time)
            //    - APPOINTMENT_RESCHEDULE_CANCELLED (patient rejected new time, appointment cancelled)
            try {
                NotificationType notificationType = null;
                if ("confirm".equalsIgnoreCase(action)) {
                    notificationType = NotificationType.APPOINTMENT_RESCHEDULE_CONFIRMED;
                } else if ("cancel".equalsIgnoreCase(action)) {
                    notificationType = NotificationType.APPOINTMENT_RESCHEDULE_CANCELLED;
                }
                
                if (notificationType != null) {
                    notificationService.createRescheduleResponseNotification(
                        patient, apt.getHealthcareProvider(), notificationType, (long) apt.getId());
                }
            } catch (Exception e) {
                // Log but don't fail the update if notification fails
                System.err.println("Failed to create reschedule response notification: " + e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Appointment " + (action.equalsIgnoreCase("confirm") ? "confirmed" : "cancelled"));
            response.put("appointmentId", apt.getId());
            response.put("newStatus", apt.getStatus().name().toLowerCase());
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }

    /**
     * Complete Appointment
     * 
     * This method allows doctors to mark an appointment as COMPLETED after the patient visit.
     * Features:
     * - Updates appointment status to COMPLETED
     * - Adds doctor's notes about the visit
     * - Optionally creates a follow-up appointment
     * - Sends notification to patient about completion
     * 
     * Only works for appointments with status CONFIRMED (upcoming appointments).
     */
    @Override
    public Map<String, Object> completeAppointment(String token, Integer appointmentId, String notes, String followUpDateTime) {
        try {
            String jwtToken = token != null && token.startsWith("Bearer ")
                ? token.substring(7)
                : token;

            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider doctor = healthcareProviderRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

            AppointmentEntity apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

            if (!apt.getHealthcareProvider().getId().equals(doctor.getId())) {
                throw new RuntimeException("You are not allowed to modify this appointment");
            }

            // VALIDATION: Only allow completing CONFIRMED appointments
            // This ensures only upcoming appointments can be completed
            if (!apt.getStatus().equals(AppointmentStatus.CONFIRMED)) {
                throw new RuntimeException("Only confirmed appointments can be completed. Current status: " + apt.getStatus());
            }

            // Update appointment to COMPLETED status
            apt.setStatus(AppointmentStatus.COMPLETED);
            
            // Add completion notes (combine with existing notes if any)
            String completionNotes = notes != null && !notes.trim().isEmpty() ? notes.trim() : null;
            if (completionNotes != null) {
                String existingNotes = apt.getNotes() != null ? apt.getNotes() : "";
                if (!existingNotes.isEmpty()) {
                    apt.setNotes(existingNotes + "\n\n--- Appointment Completion Notes ---\n" + completionNotes);
                } else {
                    apt.setNotes("--- Appointment Completion Notes ---\n" + completionNotes);
                }
            }

            appointmentRepository.save(apt);

            // CREATE FOLLOW-UP APPOINTMENT (optional)
            // If doctor provides a followUpDateTime, create a new appointment for follow-up
            AppointmentEntity followUpAppointment = null;
            if (followUpDateTime != null && !followUpDateTime.trim().isEmpty()) {
                try {
                    // Parse follow-up date/time
                    Date followUpDate;
                    try {
                        String cleanedDateTime = followUpDateTime.replace("Z", "").replace("z", "");
                        LocalDateTime localDateTime = LocalDateTime.parse(cleanedDateTime);
                        followUpDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                    } catch (Exception e) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            followUpDate = sdf.parse(followUpDateTime);
                        } catch (Exception e2) {
                            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            followUpDate = sdf2.parse(followUpDateTime);
                        }
                    }

                    // Validate follow-up date is in the future
                    if (followUpDate.before(new Date())) {
                        throw new RuntimeException("Follow-up appointment date must be in the future");
                    }

                    // Create follow-up appointment
                    followUpAppointment = new AppointmentEntity();
                    followUpAppointment.setPatient(apt.getPatient());
                    followUpAppointment.setHealthcareProvider(apt.getHealthcareProvider());
                    followUpAppointment.setAppointmentDateTime(followUpDate);
                    followUpAppointment.setStatus(AppointmentStatus.PENDING); // New appointment starts as PENDING
                    followUpAppointment.setType(AppointmentType.FOLLOW_UP); // Mark as follow-up appointment
                    followUpAppointment.setReason("Follow-up appointment after visit on " + 
                        new SimpleDateFormat("MMM dd, yyyy").format(apt.getAppointmentDateTime()));
                    
                    // Inherit medical records sharing preference from original appointment
                    followUpAppointment.setShareMedicalRecords(apt.getShareMedicalRecords());

                    followUpAppointment = appointmentRepository.save(followUpAppointment);

                    // NOTIFICATION: Notify doctor about new follow-up appointment request
                    // (This is automatic since it's a PENDING appointment)
                    try {
                        notificationService.createAppointmentRequestedNotification(
                            apt.getPatient(), doctor, (long) followUpAppointment.getId());
                    } catch (Exception e) {
                        System.err.println("Failed to create follow-up appointment notification: " + e.getMessage());
                    }

                    System.out.println("Follow-up appointment created: ID " + followUpAppointment.getId());

                } catch (Exception e) {
                    // Log error but don't fail the completion if follow-up creation fails
                    System.err.println("Failed to create follow-up appointment: " + e.getMessage());
                }
            }

            // NOTIFICATION: Notify patient that appointment is completed
            // This creates an APPOINTMENT_COMPLETED notification (if that type exists)
            // For now, we'll use a generic notification or extend NotificationType
            try {
                // You can add APPOINTMENT_COMPLETED to NotificationType enum if needed
                // For now, we'll skip notification for completion
                // notificationService.createAppointmentStatusNotification(
                //     doctor, apt.getPatient(), NotificationType.APPOINTMENT_COMPLETED, (long) apt.getId(), null);
            } catch (Exception e) {
                System.err.println("Failed to create completion notification: " + e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Appointment marked as completed");
            response.put("appointmentId", apt.getId());
            response.put("newStatus", "completed");
            
            // Include follow-up appointment ID if one was created
            if (followUpAppointment != null) {
                response.put("followUpAppointmentId", followUpAppointment.getId());
            }
            
            return response;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }

    /**
     * Start video call for an appointment
     * Sets isCallActive flag to true so patient can join
     */
    public Map<String, Object> startCall(String token, Integer appointmentId) {
        try {
            String jwtToken = token != null && token.startsWith("Bearer ")
                ? token.substring(7)
                : token;

            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider doctor = healthcareProviderRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

            AppointmentEntity apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

            if (!apt.getHealthcareProvider().getId().equals(doctor.getId())) {
                throw new RuntimeException("You are not allowed to start this appointment call");
            }

            if (!Boolean.TRUE.equals(apt.getIsVideoCall())) {
                throw new RuntimeException("This is not a video call appointment");
            }

            if (!apt.getStatus().equals(AppointmentStatus.CONFIRMED)) {
                throw new RuntimeException("Only confirmed appointments can be started");
            }

            // Set call as active
            apt.setIsCallActive(true);
            appointmentRepository.save(apt);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Video call started");
            response.put("appointmentId", apt.getId());
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }

    /**
     * End video call for an appointment
     * Sets isCallActive flag to false
     */
    public Map<String, Object> endCall(String token, Integer appointmentId) {
        try {
            String jwtToken = token != null && token.startsWith("Bearer ")
                ? token.substring(7)
                : token;

            String username = jwtService.extractUserName(jwtToken);
            HealthcareProvider doctor = healthcareProviderRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

            AppointmentEntity apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

            if (!apt.getHealthcareProvider().getId().equals(doctor.getId())) {
                throw new RuntimeException("You are not allowed to end this appointment call");
            }

            // Set call as inactive
            apt.setIsCallActive(false);
            appointmentRepository.save(apt);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Video call ended");
            response.put("appointmentId", apt.getId());
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }

    /**
     * Get available time slots for a doctor on a specific date.
     * Checks confirmed appointments and marks those time slots as unavailable.
     * 
     * @param doctorId The doctor's ID
     * @param date The date to check availability for (ISO date string: YYYY-MM-DD)
     * @param startTime Doctor's available start time (HH:mm format)
     * @param endTime Doctor's available end time (HH:mm format)
     * @return Map with status and list of time slots with availability
     */
    @Override
    public Map<String, Object> getAvailableTimeSlots(Long doctorId, String date, String startTime, String endTime) {
        try {
            // Verify doctor exists
            healthcareProviderRepo.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

            // Parse the date using system default timezone (same as appointments are stored)
            Date appointmentDate;
            try {
                // Parse ISO date string (YYYY-MM-DD)
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                appointmentDate = sdf.parse(date);
            } catch (Exception e) {
                throw new RuntimeException("Invalid date format. Expected YYYY-MM-DD: " + e.getMessage());
            }

            // Calculate start and end of day for the date query
            // Use system default timezone to match how appointments are stored
            Calendar startOfDay = Calendar.getInstance();
            startOfDay.setTime(appointmentDate);
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);
            
            Calendar endOfDay = Calendar.getInstance();
            endOfDay.setTime(appointmentDate);
            endOfDay.add(Calendar.DAY_OF_MONTH, 1); // Next day
            endOfDay.set(Calendar.HOUR_OF_DAY, 0);
            endOfDay.set(Calendar.MINUTE, 0);
            endOfDay.set(Calendar.SECOND, 0);
            endOfDay.set(Calendar.MILLISECOND, 0);
            
            // Get confirmed appointments for this doctor on this date
            System.out.println("=== FETCHING AVAILABLE SLOTS ===");
            System.out.println("Doctor ID: " + doctorId);
            System.out.println("Date: " + date);
            System.out.println("Start of day: " + startOfDay.getTime());
            System.out.println("End of day: " + endOfDay.getTime());
            
            // Get all appointments for this doctor, then filter by date and status
            // Temporary workaround: filter in Java instead of SQL query
            List<AppointmentEntity> allDoctorAppointments = appointmentRepository.findByHealthcareProviderId(doctorId);
            
            // Filter for confirmed appointments on the selected date
            List<AppointmentEntity> confirmedAppointments = new ArrayList<>();
            for (AppointmentEntity apt : allDoctorAppointments) {
                if (apt.getStatus() == AppointmentStatus.CONFIRMED && apt.getAppointmentDateTime() != null) {
                    Calendar aptCal = Calendar.getInstance();
                    aptCal.setTime(apt.getAppointmentDateTime());
                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(startOfDay.getTime());
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(endOfDay.getTime());
                    
                    // Check if appointment is on the same day
                    if (aptCal.get(Calendar.YEAR) == startCal.get(Calendar.YEAR) &&
                        aptCal.get(Calendar.DAY_OF_YEAR) == startCal.get(Calendar.DAY_OF_YEAR)) {
                        confirmedAppointments.add(apt);
                    }
                }
            }

            System.out.println("Found " + confirmedAppointments.size() + " confirmed appointments for this date");

            // Extract booked time slots
            Set<String> bookedSlots = new HashSet<>();
            for (AppointmentEntity apt : confirmedAppointments) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(apt.getAppointmentDateTime());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);
                String bookedTime = String.format("%02d:%02d", hour, minute);
                System.out.println("Booked slot found: " + bookedTime + " (appointment ID: " + apt.getId() + ")");
                bookedSlots.add(bookedTime);
            }
            
            System.out.println("Total booked slots: " + bookedSlots.size());
            System.out.println("Booked slots: " + bookedSlots);

            // Generate all possible time slots
            List<Map<String, Object>> timeSlots = new ArrayList<>();
            Calendar slotStart = Calendar.getInstance();
            slotStart.setTime(appointmentDate);
            
            // Parse start and end times
            String[] startParts = startTime.split(":");
            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            
            String[] endParts = endTime.split(":");
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);
            
            slotStart.set(Calendar.HOUR_OF_DAY, startHour);
            slotStart.set(Calendar.MINUTE, startMinute);
            slotStart.set(Calendar.SECOND, 0);
            slotStart.set(Calendar.MILLISECOND, 0);
            
            Calendar slotEnd = Calendar.getInstance();
            slotEnd.setTime(appointmentDate);
            slotEnd.set(Calendar.HOUR_OF_DAY, endHour);
            slotEnd.set(Calendar.MINUTE, endMinute);
            slotEnd.set(Calendar.SECOND, 0);
            slotEnd.set(Calendar.MILLISECOND, 0);
            
            // Generate 30-minute slots
            while (slotStart.before(slotEnd)) {
                int hour = slotStart.get(Calendar.HOUR_OF_DAY);
                int minute = slotStart.get(Calendar.MINUTE);
                String timeString = String.format("%02d:%02d", hour, minute);
                
                boolean isAvailable = !bookedSlots.contains(timeString);
                
                Map<String, Object> slot = new HashMap<>();
                slot.put("time", timeString);
                slot.put("available", isAvailable);
                
                if (!isAvailable) {
                    System.out.println("Slot " + timeString + " is UNAVAILABLE (booked)");
                }
                
                timeSlots.add(slot);
                
                // Move to next 30-minute slot
                slotStart.add(Calendar.MINUTE, 30);
            }

            System.out.println("Generated " + timeSlots.size() + " time slots");
            System.out.println("Available slots: " + timeSlots.stream().filter(s -> (Boolean)s.get("available")).count());
            System.out.println("Unavailable slots: " + timeSlots.stream().filter(s -> !(Boolean)s.get("available")).count());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", timeSlots);
            return response;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }
}

