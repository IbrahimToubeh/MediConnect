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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepo patientRepo;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final JWTService jwtService;
    private final NotificationService notificationService;

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
            
            // Set shareMedicalRecords - default to false if not provided
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

            appointment = appointmentRepository.save(appointment);

            // Notify doctor about new appointment request
            try {
                notificationService.createAppointmentRequestedNotification(patient, doctor, (long) appointment.getId());
            } catch (Exception e) {
                // Log but don't fail the appointment booking if notification fails
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

                if (apt.getAppointmentDateTime() != null) {
                    aptMap.put("appointmentDateTime", apt.getAppointmentDateTime().toInstant().toString());
                    aptMap.put("date", dateFormat.format(apt.getAppointmentDateTime()));
                    aptMap.put("time", timeFormat.format(apt.getAppointmentDateTime()));
                }

                aptMap.put("description", apt.getReason() != null ? apt.getReason() : "");
                aptMap.put("shareMedicalRecords", apt.getShareMedicalRecords() != null ? apt.getShareMedicalRecords() : false);
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

                if (apt.getAppointmentDateTime() != null) {
                    aptMap.put("appointmentDateTime", apt.getAppointmentDateTime().toInstant().toString());
                    aptMap.put("date", dateFormat.format(apt.getAppointmentDateTime()));
                    aptMap.put("time", timeFormat.format(apt.getAppointmentDateTime()));
                }

                aptMap.put("description", apt.getReason() != null ? apt.getReason() : "");
                aptMap.put("shareMedicalRecords", apt.getShareMedicalRecords() != null ? apt.getShareMedicalRecords() : false);
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
    public Map<String, Object> updateAppointmentStatus(String token, Long appointmentId, String status, String note, String newDateTime) {
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

            AppointmentStatus newStatus = AppointmentStatus.valueOf(status.toUpperCase());
            apt.setStatus(newStatus);
            
            // If rescheduling, update the appointment date/time
            if ("RESCHEDULED".equals(status) && newDateTime != null && !newDateTime.isEmpty()) {
                try {
                    String cleanedDateTime = newDateTime.replace("Z", "").replace("z", "");
                    LocalDateTime localDateTime = LocalDateTime.parse(cleanedDateTime);
                    Date appointmentDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                    apt.setAppointmentDateTime(appointmentDate);
                } catch (Exception e) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        apt.setAppointmentDateTime(sdf.parse(newDateTime));
                    } catch (Exception e2) {
                        throw new RuntimeException("Invalid date format for rescheduling", e2);
                    }
                }
            }
            
            if (note != null && !note.isBlank()) {
                apt.setNotes(note);
            }

            appointmentRepository.save(apt);

            // Notify patient about appointment status change
            try {
                NotificationType notificationType = null;
                String additionalInfo = null;
                
                if ("CONFIRMED".equals(status)) {
                    notificationType = NotificationType.APPOINTMENT_CONFIRMED;
                } else if ("CANCELLED".equals(status)) {
                    notificationType = NotificationType.APPOINTMENT_CANCELLED;
                } else if ("RESCHEDULED".equals(status)) {
                    notificationType = NotificationType.APPOINTMENT_RESCHEDULED;
                    // Include new date/time in notification if available
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
    public Map<String, Object> respondToReschedule(String token, Long appointmentId, String action) {
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

            if (!apt.getStatus().equals(AppointmentStatus.RESCHEDULED)) {
                throw new RuntimeException("Appointment is not in rescheduled status");
            }

            if ("confirm".equalsIgnoreCase(action)) {
                apt.setStatus(AppointmentStatus.CONFIRMED);
            } else if ("cancel".equalsIgnoreCase(action)) {
                apt.setStatus(AppointmentStatus.CANCELLED);
            } else {
                throw new RuntimeException("Invalid action. Use 'confirm' or 'cancel'");
            }

            appointmentRepository.save(apt);

            // Notify doctor about patient's response to reschedule
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
}

