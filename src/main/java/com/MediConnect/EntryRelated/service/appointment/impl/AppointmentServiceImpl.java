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
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset; // Added UTC
import java.util.*;

/**
 * Appointment Service Implementation
 * Handles appointment booking, retrieval, and status updates with integrated notification system.
 */
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepo patientRepo;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final JWTService jwtService;
    private final NotificationService notificationService;
    private final ChatService chatService;

    @Override
    @Transactional
    public Map<String, Object> bookAppointment(String token, Map<String, Object> request) {
        try {
            String jwtToken = token != null && token.startsWith("Bearer ")
                    ? token.substring(7)
                    : token;

            String username = jwtService.extractUserName(jwtToken);
            Patient patient = patientRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            Long doctorId = Long.parseLong(request.get("doctorId").toString());
            HealthcareProvider doctor = healthcareProviderRepo.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            if (request.get("appointmentDateTime") == null) {
                throw new RuntimeException("Appointment date and time is required");
            }
            String dateTimeStr = request.get("appointmentDateTime").toString();
            Date appointmentDateTime;

            // ✅ FIX: Force UTC Parsing
            try {
                // Handle ISO format (e.g., "2025-11-22T14:30:00.000Z")
                String cleanedDateTime = dateTimeStr.replace("Z", "").replace("z", "");
                LocalDateTime localDateTime = LocalDateTime.parse(cleanedDateTime);
                // Convert using UTC Zone
                appointmentDateTime = Date.from(localDateTime.toInstant(ZoneOffset.UTC));
            } catch (Exception e) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Force UTC
                    appointmentDateTime = sdf.parse(dateTimeStr);
                } catch (Exception e2) {
                    try {
                        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        sdf2.setTimeZone(TimeZone.getTimeZone("UTC")); // Force UTC
                        appointmentDateTime = sdf2.parse(dateTimeStr);
                    } catch (Exception e3) {
                        throw new RuntimeException("Invalid appointment date format: " + dateTimeStr, e3);
                    }
                }
            }

            if (appointmentDateTime.before(new Date())) {
                throw new RuntimeException("Appointment date must be in the future");
            }

            AppointmentEntity appointment = new AppointmentEntity();
            appointment.setPatient(patient);
            appointment.setHealthcareProvider(doctor);
            appointment.setAppointmentDateTime(appointmentDateTime);
            appointment.setStatus(AppointmentStatus.PENDING);
            appointment.setType(AppointmentType.CONSULTATION);
            appointment.setReason(request.get("description") != null ? request.get("description").toString() : "");

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

            Boolean isVideoCall = false;
            if (request.get("isVideoCall") != null) {
                Object videoCallValue = request.get("isVideoCall");
                if (videoCallValue instanceof Boolean) {
                    isVideoCall = (Boolean) videoCallValue;
                } else if (videoCallValue instanceof String) {
                    isVideoCall = Boolean.parseBoolean((String) videoCallValue);
                }
            }

            boolean isPsychiatrist = doctor.getSpecializations() != null &&
                    doctor.getSpecializations().stream()
                            .anyMatch(spec -> spec != null && spec.name().equals("PSYCHIATRY"));

            if (isVideoCall && !isPsychiatrist) {
                throw new RuntimeException("Video call appointments are only available for psychiatry doctors");
            }

            appointment.setIsVideoCall(isVideoCall);

            appointment = appointmentRepository.save(appointment);

            try {
                notificationService.createAppointmentRequestedNotification(
                        patient.getId(),
                        doctor.getId(),
                        (long) appointment.getId()
                );
            } catch (Exception e) {
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
    @Transactional(readOnly = true)
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
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Force UTC output

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            timeFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Force UTC output

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
                aptMap.put("doctorProfilePicture", apt.getHealthcareProvider().getProfilePicture());

                if (apt.getAppointmentDateTime() != null) {
                    // Ensure output is strictly ISO-8601 UTC
                    aptMap.put("appointmentDateTime", apt.getAppointmentDateTime().toInstant().toString());
                    aptMap.put("date", dateFormat.format(apt.getAppointmentDateTime()));
                    aptMap.put("time", timeFormat.format(apt.getAppointmentDateTime()));
                }

                aptMap.put("description", apt.getReason() != null ? apt.getReason() : "");
                aptMap.put("shareMedicalRecords", apt.getShareMedicalRecords() != null ? apt.getShareMedicalRecords() : false);
                aptMap.put("isVideoCall", apt.getIsVideoCall() != null ? apt.getIsVideoCall() : false);
                aptMap.put("isCallActive", apt.getIsCallActive() != null ? apt.getIsCallActive() : false);

                aptMap.put("insuranceProvider", apt.getPatient().getInsuranceProvider());
                aptMap.put("insuranceNumber", apt.getPatient().getInsuranceNumber());

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
    @Transactional(readOnly = true)
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
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

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
                aptMap.put("doctorProfilePicture", apt.getHealthcareProvider().getProfilePicture());

                if (apt.getAppointmentDateTime() != null) {
                    aptMap.put("appointmentDateTime", apt.getAppointmentDateTime().toInstant().toString());
                    aptMap.put("date", dateFormat.format(apt.getAppointmentDateTime()));
                    aptMap.put("time", timeFormat.format(apt.getAppointmentDateTime()));
                }

                aptMap.put("description", apt.getReason() != null ? apt.getReason() : "");
                aptMap.put("shareMedicalRecords", apt.getShareMedicalRecords() != null ? apt.getShareMedicalRecords() : false);
                aptMap.put("isVideoCall", apt.getIsVideoCall() != null ? apt.getIsVideoCall() : false);
                aptMap.put("isCallActive", apt.getIsCallActive() != null ? apt.getIsCallActive() : false);

                aptMap.put("insuranceProvider", apt.getPatient().getInsuranceProvider());
                aptMap.put("insuranceNumber", apt.getPatient().getInsuranceNumber());

                if (Boolean.TRUE.equals(apt.getShareMedicalRecords())) {
                    Map<String, Object> med = new HashMap<>();
                    med.put("allergies", apt.getPatient().getAllergies());
                    med.put("medicalConditions", apt.getPatient().getMedicalConditions());
                    med.put("previousSurgeries", apt.getPatient().getPreviousSurgeries());

                    // Add Lab Results
                    List<Map<String, Object>> labResults = new ArrayList<>();
                    if (apt.getPatient().getLaboratoryResults() != null) {
                        for (com.MediConnect.EntryRelated.entities.LaboratoryResult lab : apt.getPatient().getLaboratoryResults()) {
                            Map<String, Object> labMap = new HashMap<>();
                            labMap.put("id", lab.getId());
                            labMap.put("description", lab.getDescription());
                            labMap.put("hasImage", lab.getImage() != null && lab.getImage().length > 0);
                            labMap.put("imageSize", lab.getImage() != null ? lab.getImage().length : 0);
                            labMap.put("resultUrl", lab.getResultUrl());
                            labResults.add(labMap);
                        }
                    }
                    med.put("labResults", labResults);

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
    @Transactional
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

            AppointmentStatus newStatus = AppointmentStatus.valueOf(status.toUpperCase());
            apt.setStatus(newStatus);

            if ("RESCHEDULED".equals(status) && newDateTime != null && !newDateTime.isEmpty()) {
                try {
                    String cleanedDateTime = newDateTime.replace("Z", "").replace("z", "");
                    LocalDateTime localDateTime = LocalDateTime.parse(cleanedDateTime);
                    // Force UTC
                    Date appointmentDate = Date.from(localDateTime.toInstant(ZoneOffset.UTC));
                    apt.setAppointmentDateTime(appointmentDate);
                    apt.setReminder24hSent(false);
                } catch (Exception e) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        apt.setAppointmentDateTime(sdf.parse(newDateTime));
                        apt.setReminder24hSent(false);
                    } catch (Exception e2) {
                        throw new RuntimeException("Invalid date format for rescheduling", e2);
                    }
                }
            }

            if (note != null && !note.isBlank()) {
                apt.setNotes(note);
            }

            apt = appointmentRepository.save(apt);

            if ("CONFIRMED".equalsIgnoreCase(status) || apt.getStatus() == AppointmentStatus.CONFIRMED) {
                try {
                    System.out.println("DEBUG: Creating chat channel for confirmed appointment ID: " + apt.getId());
                    chatService.createChannelForConfirmedAppointment(apt);
                    System.out.println("SUCCESS: Chat channel created for confirmed appointment ID: " + apt.getId());
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to create chat channel: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            try {
                NotificationType notificationType = null;
                String additionalInfo = null;

                if ("CONFIRMED".equals(status)) {
                    notificationType = NotificationType.APPOINTMENT_CONFIRMED;
                } else if ("CANCELLED".equals(status)) {
                    notificationType = NotificationType.APPOINTMENT_CANCELLED;
                } else if ("RESCHEDULED".equals(status)) {
                    notificationType = NotificationType.APPOINTMENT_RESCHEDULED;
                    if (apt.getAppointmentDateTime() != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
                        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Consistent time zone
                        additionalInfo = "New time: " + dateFormat.format(apt.getAppointmentDateTime()) + " (UTC)";
                    }
                }

                if (notificationType != null) {
                    notificationService.createAppointmentStatusNotification(
                            doctor.getId(),
                            apt.getPatient().getId(),
                            notificationType,
                            (long) apt.getId(),
                            additionalInfo
                    );
                }
            } catch (Exception e) {
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
    @Transactional
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

            try {
                NotificationType notificationType = null;
                if ("confirm".equalsIgnoreCase(action)) {
                    notificationType = NotificationType.APPOINTMENT_RESCHEDULE_CONFIRMED;
                } else if ("cancel".equalsIgnoreCase(action)) {
                    notificationType = NotificationType.APPOINTMENT_RESCHEDULE_CANCELLED;
                }

                if (notificationType != null) {
                    notificationService.createRescheduleResponseNotification(
                            patient.getId(),
                            apt.getHealthcareProvider().getId(),
                            notificationType,
                            (long) apt.getId()
                    );
                }
            } catch (Exception e) {
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

    @Override
    @Transactional
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

            if (!apt.getStatus().equals(AppointmentStatus.CONFIRMED)) {
                throw new RuntimeException("Only confirmed appointments can be completed. Current status: " + apt.getStatus());
            }

            apt.setStatus(AppointmentStatus.COMPLETED);

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

            AppointmentEntity followUpAppointment = null;
            if (followUpDateTime != null && !followUpDateTime.trim().isEmpty()) {
                try {
                    Date followUpDate;
                    try {
                        String cleanedDateTime = followUpDateTime.replace("Z", "").replace("z", "");
                        LocalDateTime localDateTime = LocalDateTime.parse(cleanedDateTime);
                        // Force UTC
                        followUpDate = Date.from(localDateTime.toInstant(ZoneOffset.UTC));
                    } catch (Exception e) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                            followUpDate = sdf.parse(followUpDateTime);
                        } catch (Exception e2) {
                            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
                            followUpDate = sdf2.parse(followUpDateTime);
                        }
                    }

                    if (followUpDate.before(new Date())) {
                        throw new RuntimeException("Follow-up appointment date must be in the future");
                    }

                    followUpAppointment = new AppointmentEntity();
                    followUpAppointment.setPatient(apt.getPatient());
                    followUpAppointment.setHealthcareProvider(apt.getHealthcareProvider());
                    followUpAppointment.setAppointmentDateTime(followUpDate);
                    followUpAppointment.setStatus(AppointmentStatus.PENDING);
                    followUpAppointment.setType(AppointmentType.FOLLOW_UP);

                    SimpleDateFormat msgFormat = new SimpleDateFormat("MMM dd, yyyy");
                    msgFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    followUpAppointment.setReason("Follow-up appointment after visit on " +
                            msgFormat.format(apt.getAppointmentDateTime()));

                    followUpAppointment.setShareMedicalRecords(apt.getShareMedicalRecords());

                    followUpAppointment = appointmentRepository.save(followUpAppointment);

                    try {
                        notificationService.createAppointmentRequestedNotification(
                                apt.getPatient().getId(),
                                doctor.getId(),
                                (long) followUpAppointment.getId()
                        );
                    } catch (Exception e) {
                        System.err.println("Failed to create follow-up appointment notification: " + e.getMessage());
                    }

                    System.out.println("Follow-up appointment created: ID " + followUpAppointment.getId());

                } catch (Exception e) {
                    System.err.println("Failed to create follow-up appointment: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Appointment marked as completed");
            response.put("appointmentId", apt.getId());
            response.put("newStatus", "completed");

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

    @Override
    @Transactional
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

    @Override
    @Transactional
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailableTimeSlots(Long doctorId, String date, String startTime, String endTime) {
        try {
            healthcareProviderRepo.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            Date appointmentDate;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Force UTC
                appointmentDate = sdf.parse(date);
            } catch (Exception e) {
                throw new RuntimeException("Invalid date format. Expected YYYY-MM-DD: " + e.getMessage());
            }

            // Use UTC Calendar
            Calendar startOfDay = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            startOfDay.setTime(appointmentDate);
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);

            Calendar endOfDay = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            endOfDay.setTime(appointmentDate);
            endOfDay.add(Calendar.DAY_OF_MONTH, 1);
            endOfDay.set(Calendar.HOUR_OF_DAY, 0);
            endOfDay.set(Calendar.MINUTE, 0);
            endOfDay.set(Calendar.SECOND, 0);
            endOfDay.set(Calendar.MILLISECOND, 0);

            List<AppointmentEntity> allDoctorAppointments = appointmentRepository.findByHealthcareProviderId(doctorId);

            List<AppointmentEntity> confirmedAppointments = new ArrayList<>();
            for (AppointmentEntity apt : allDoctorAppointments) {
                if (apt.getStatus() == AppointmentStatus.CONFIRMED && apt.getAppointmentDateTime() != null) {
                    Calendar aptCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    aptCal.setTime(apt.getAppointmentDateTime());

                    // Compare using UTC calendars
                    if (aptCal.get(Calendar.YEAR) == startOfDay.get(Calendar.YEAR) &&
                            aptCal.get(Calendar.DAY_OF_YEAR) == startOfDay.get(Calendar.DAY_OF_YEAR)) {
                        confirmedAppointments.add(apt);
                    }
                }
            }

            Set<String> bookedSlots = new HashSet<>();
            for (AppointmentEntity apt : confirmedAppointments) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTime(apt.getAppointmentDateTime());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);
                String bookedTime = String.format("%02d:%02d", hour, minute);
                bookedSlots.add(bookedTime);
            }

            List<Map<String, Object>> timeSlots = new ArrayList<>();
            Calendar slotStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            slotStart.setTime(appointmentDate);

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

            Calendar slotEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            slotEnd.setTime(appointmentDate);
            slotEnd.set(Calendar.HOUR_OF_DAY, endHour);
            slotEnd.set(Calendar.MINUTE, endMinute);
            slotEnd.set(Calendar.SECOND, 0);
            slotEnd.set(Calendar.MILLISECOND, 0);

            while (slotStart.before(slotEnd)) {
                int hour = slotStart.get(Calendar.HOUR_OF_DAY);
                int minute = slotStart.get(Calendar.MINUTE);
                String timeString = String.format("%02d:%02d", hour, minute);

                boolean isAvailable = !bookedSlots.contains(timeString);

                Map<String, Object> slot = new HashMap<>();
                slot.put("time", timeString);
                slot.put("available", isAvailable);

                timeSlots.add(slot);
                slotStart.add(Calendar.MINUTE, 30);
            }

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