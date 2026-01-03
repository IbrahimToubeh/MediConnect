package com.MediConnect.EntryRelated.service.appointment.impl;

import com.MediConnect.Entities.AppointmentEntity;
import com.MediConnect.Entities.AppointmentStatus;
import com.MediConnect.Entities.AppointmentType;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.repository.AppointmentRepository;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.repository.DayAvailabilityRepository;
import com.MediConnect.EntryRelated.repository.BlockedTimeSlotRepository;
import com.MediConnect.EntryRelated.entities.DayAvailability;
import com.MediConnect.EntryRelated.entities.BlockedTimeSlot;
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
    private final DayAvailabilityRepository dayAvailabilityRepository;
    private final BlockedTimeSlotRepository blockedTimeSlotRepository;
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
                    
                    // Basic Information
                    med.put("gender", apt.getPatient().getGender());
                    if (apt.getPatient().getDateOfBirth() != null) {
                        SimpleDateFormat dobFormat = new SimpleDateFormat("yyyy-MM-dd");
                        dobFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        med.put("dateOfBirth", dobFormat.format(apt.getPatient().getDateOfBirth()));
                    } else {
                        med.put("dateOfBirth", null);
                    }
                    med.put("height", apt.getPatient().getHeight());
                    med.put("weight", apt.getPatient().getWeight());
                    med.put("bloodType", apt.getPatient().getBloodType() != null ? apt.getPatient().getBloodType().toString() : null);
                    
                    // Medical Information
                    med.put("allergies", apt.getPatient().getAllergies());
                    med.put("medicalConditions", apt.getPatient().getMedicalConditions());
                    med.put("previousSurgeries", apt.getPatient().getPreviousSurgeries());
                    med.put("familyMedicalHistory", apt.getPatient().getFamilyMedicalHistory());
                    
                    // Lifestyle Information
                    med.put("dietaryHabits", apt.getPatient().getDietaryHabits() != null ? apt.getPatient().getDietaryHabits().toString() : null);
                    med.put("alcoholConsumption", apt.getPatient().getAlcoholConsumption() != null ? apt.getPatient().getAlcoholConsumption().toString() : null);
                    med.put("physicalActivity", apt.getPatient().getPhysicalActivity() != null ? apt.getPatient().getPhysicalActivity().toString() : null);
                    med.put("smokingStatus", apt.getPatient().getSmokingStatus() != null ? apt.getPatient().getSmokingStatus().toString() : null);
                    med.put("mentalHealthCondition", apt.getPatient().getMentalHealthCondition() != null ? apt.getPatient().getMentalHealthCondition().toString() : null);
                    
                    // Current Medications
                    List<Map<String, Object>> medications = new ArrayList<>();
                    if (apt.getPatient().getMedications() != null) {
                        SimpleDateFormat medDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        medDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        for (com.MediConnect.EntryRelated.entities.Medication medication : apt.getPatient().getMedications()) {
                            Map<String, Object> medMap = new HashMap<>();
                            medMap.put("id", medication.getId());
                            medMap.put("medicationName", medication.getMedicationName());
                            medMap.put("medicationDosage", medication.getMedicationDosage());
                            medMap.put("medicationFrequency", medication.getMedicationFrequency());
                            medMap.put("medicationStartDate", medication.getMedicationStartDate() != null ? medDateFormat.format(medication.getMedicationStartDate()) : null);
                            medMap.put("medicationEndDate", medication.getMedicationEndDate() != null ? medDateFormat.format(medication.getMedicationEndDate()) : null);
                            medMap.put("inUse", medication.isInUse());
                            medications.add(medMap);
                        }
                    }
                    med.put("medications", medications);
                    
                    // Mental Health Medications
                    List<Map<String, Object>> mentalHealthMedications = new ArrayList<>();
                    if (apt.getPatient().getMentalHealthMedications() != null) {
                        SimpleDateFormat medDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        medDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        for (com.MediConnect.EntryRelated.entities.MentalHealthMedication medication : apt.getPatient().getMentalHealthMedications()) {
                            Map<String, Object> medMap = new HashMap<>();
                            medMap.put("id", medication.getId());
                            medMap.put("medicationName", medication.getMedicationName());
                            medMap.put("medicationDosage", medication.getMedicationDosage());
                            medMap.put("medicationFrequency", medication.getMedicationFrequency());
                            medMap.put("medicationStartDate", medication.getMedicationStartDate() != null ? medDateFormat.format(medication.getMedicationStartDate()) : null);
                            medMap.put("medicationEndDate", medication.getMedicationEndDate() != null ? medDateFormat.format(medication.getMedicationEndDate()) : null);
                            medMap.put("inUse", medication.isInUse());
                            mentalHealthMedications.add(medMap);
                        }
                    }
                    med.put("mentalHealthMedications", mentalHealthMedications);
                    
                    // Lab Results
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
                    
                    // Insurance Information
                    med.put("insuranceProvider", apt.getPatient().getInsuranceProvider());
                    med.put("insuranceNumber", apt.getPatient().getInsuranceNumber());
                    
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
                    
                    // Basic Information
                    med.put("gender", apt.getPatient().getGender());
                    if (apt.getPatient().getDateOfBirth() != null) {
                        SimpleDateFormat dobFormat = new SimpleDateFormat("yyyy-MM-dd");
                        dobFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        med.put("dateOfBirth", dobFormat.format(apt.getPatient().getDateOfBirth()));
                    } else {
                        med.put("dateOfBirth", null);
                    }
                    med.put("height", apt.getPatient().getHeight());
                    med.put("weight", apt.getPatient().getWeight());
                    med.put("bloodType", apt.getPatient().getBloodType() != null ? apt.getPatient().getBloodType().toString() : null);
                    
                    // Medical Information
                    med.put("allergies", apt.getPatient().getAllergies());
                    med.put("medicalConditions", apt.getPatient().getMedicalConditions());
                    med.put("previousSurgeries", apt.getPatient().getPreviousSurgeries());
                    med.put("familyMedicalHistory", apt.getPatient().getFamilyMedicalHistory());
                    
                    // Lifestyle Information
                    med.put("dietaryHabits", apt.getPatient().getDietaryHabits() != null ? apt.getPatient().getDietaryHabits().toString() : null);
                    med.put("alcoholConsumption", apt.getPatient().getAlcoholConsumption() != null ? apt.getPatient().getAlcoholConsumption().toString() : null);
                    med.put("physicalActivity", apt.getPatient().getPhysicalActivity() != null ? apt.getPatient().getPhysicalActivity().toString() : null);
                    med.put("smokingStatus", apt.getPatient().getSmokingStatus() != null ? apt.getPatient().getSmokingStatus().toString() : null);
                    med.put("mentalHealthCondition", apt.getPatient().getMentalHealthCondition() != null ? apt.getPatient().getMentalHealthCondition().toString() : null);
                    
                    // Current Medications
                    List<Map<String, Object>> medications = new ArrayList<>();
                    if (apt.getPatient().getMedications() != null) {
                        SimpleDateFormat medDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        medDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        for (com.MediConnect.EntryRelated.entities.Medication medication : apt.getPatient().getMedications()) {
                            Map<String, Object> medMap = new HashMap<>();
                            medMap.put("id", medication.getId());
                            medMap.put("medicationName", medication.getMedicationName());
                            medMap.put("medicationDosage", medication.getMedicationDosage());
                            medMap.put("medicationFrequency", medication.getMedicationFrequency());
                            medMap.put("medicationStartDate", medication.getMedicationStartDate() != null ? medDateFormat.format(medication.getMedicationStartDate()) : null);
                            medMap.put("medicationEndDate", medication.getMedicationEndDate() != null ? medDateFormat.format(medication.getMedicationEndDate()) : null);
                            medMap.put("inUse", medication.isInUse());
                            medications.add(medMap);
                        }
                    }
                    med.put("medications", medications);
                    
                    // Mental Health Medications
                    List<Map<String, Object>> mentalHealthMedications = new ArrayList<>();
                    if (apt.getPatient().getMentalHealthMedications() != null) {
                        SimpleDateFormat medDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        medDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        for (com.MediConnect.EntryRelated.entities.MentalHealthMedication medication : apt.getPatient().getMentalHealthMedications()) {
                            Map<String, Object> medMap = new HashMap<>();
                            medMap.put("id", medication.getId());
                            medMap.put("medicationName", medication.getMedicationName());
                            medMap.put("medicationDosage", medication.getMedicationDosage());
                            medMap.put("medicationFrequency", medication.getMedicationFrequency());
                            medMap.put("medicationStartDate", medication.getMedicationStartDate() != null ? medDateFormat.format(medication.getMedicationStartDate()) : null);
                            medMap.put("medicationEndDate", medication.getMedicationEndDate() != null ? medDateFormat.format(medication.getMedicationEndDate()) : null);
                            medMap.put("inUse", medication.isInUse());
                            mentalHealthMedications.add(medMap);
                        }
                    }
                    med.put("mentalHealthMedications", mentalHealthMedications);
                    
                    // Lab Results
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
                    
                    // Insurance Information
                    med.put("insuranceProvider", apt.getPatient().getInsuranceProvider());
                    med.put("insuranceNumber", apt.getPatient().getInsuranceNumber());

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
            HealthcareProvider provider = healthcareProviderRepo.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            Date appointmentDate;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Force UTC
                appointmentDate = sdf.parse(date);
            } catch (Exception e) {
                throw new RuntimeException("Invalid date format. Expected YYYY-MM-DD: " + e.getMessage());
            }

            // Get day of week
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTime(appointmentDate);
            String[] daysOfWeek = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
            String dayOfWeek = daysOfWeek[cal.get(Calendar.DAY_OF_WEEK) - 1];

            // Get day availability for this day
            DayAvailability dayAvailability = dayAvailabilityRepository
                    .findByProviderAndDayOfWeek(provider, dayOfWeek)
                    .orElse(null);

            // Check if day is available
            boolean isDayAvailable = false;
            String actualStartTime = startTime;
            String actualEndTime = endTime;
            
            if (dayAvailability != null) {
                // Day availability exists - check if enabled
                if (Boolean.TRUE.equals(dayAvailability.getEnabled())) {
                    isDayAvailable = true;
                    if (dayAvailability.getStartTime() != null && dayAvailability.getEndTime() != null) {
                        actualStartTime = dayAvailability.getStartTime();
                        actualEndTime = dayAvailability.getEndTime();
                    }
                } else {
                    // Day is explicitly disabled
                    isDayAvailable = false;
                }
            } else {
                // No day availability record - fall back to old system (availableDays)
                if (provider.getAvailableDays() != null && !provider.getAvailableDays().isEmpty()) {
                    // Check if day is in availableDays list
                    isDayAvailable = provider.getAvailableDays().stream()
                            .anyMatch(day -> day.equalsIgnoreCase(dayOfWeek));
                } else {
                    // No restrictions - allow all days
                    isDayAvailable = true;
                }
            }
            
            // If day is not available, return empty slots
            if (!isDayAvailable) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("data", new ArrayList<>()); // Return empty list
                return response;
            }

            // Get appointment duration (default 30 minutes)
            Integer appointmentDuration = provider.getAppointmentDurationMinutes() != null 
                    ? provider.getAppointmentDurationMinutes() : 30;

            // Use UTC Calendar
            Calendar startOfDay = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            startOfDay.setTime(appointmentDate);
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);

            List<AppointmentEntity> allDoctorAppointments = appointmentRepository.findByHealthcareProviderId(doctorId);

            // Include CONFIRMED, PENDING, and RESCHEDULED appointments as they all occupy time slots
            List<AppointmentEntity> bookedAppointments = new ArrayList<>();
            System.out.println("DEBUG: Checking appointments for doctorId=" + doctorId + ", date=" + date);
            System.out.println("DEBUG: Total appointments found: " + allDoctorAppointments.size());
            
            for (AppointmentEntity apt : allDoctorAppointments) {
                AppointmentStatus status = apt.getStatus();
                System.out.println("DEBUG: Checking appointment ID=" + apt.getId() + ", Status=" + status + ", DateTime=" + apt.getAppointmentDateTime());
                
                // Include appointments that occupy a time slot (confirmed, pending, or rescheduled)
                if ((status == AppointmentStatus.CONFIRMED || 
                     status == AppointmentStatus.PENDING || 
                     status == AppointmentStatus.RESCHEDULED) && 
                    apt.getAppointmentDateTime() != null) {
                    Calendar aptCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    aptCal.setTime(apt.getAppointmentDateTime());
                    
                    // Normalize to start of day for comparison
                    Calendar aptStartOfDay = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    aptStartOfDay.setTime(apt.getAppointmentDateTime());
                    aptStartOfDay.set(Calendar.HOUR_OF_DAY, 0);
                    aptStartOfDay.set(Calendar.MINUTE, 0);
                    aptStartOfDay.set(Calendar.SECOND, 0);
                    aptStartOfDay.set(Calendar.MILLISECOND, 0);

                    // Compare using UTC calendars - check if same day
                    boolean isSameDay = aptStartOfDay.getTimeInMillis() == startOfDay.getTimeInMillis();
                    
                    System.out.println("DEBUG: Appointment ID=" + apt.getId() + " - aptDate=" + aptStartOfDay.getTime() + " (" + aptStartOfDay.get(Calendar.YEAR) + "-" + (aptStartOfDay.get(Calendar.MONTH)+1) + "-" + aptStartOfDay.get(Calendar.DAY_OF_MONTH) + "), requestedDate=" + startOfDay.getTime() + " (" + startOfDay.get(Calendar.YEAR) + "-" + (startOfDay.get(Calendar.MONTH)+1) + "-" + startOfDay.get(Calendar.DAY_OF_MONTH) + "), isSameDay=" + isSameDay);
                    
                    if (isSameDay) {
                        bookedAppointments.add(apt);
                        System.out.println("DEBUG: ✓ Added appointment ID=" + apt.getId() + " (Status=" + status + ") to booked appointments for date " + date);
                    } else {
                        System.out.println("DEBUG: ✗ Appointment ID=" + apt.getId() + " is on a different day");
                    }
                } else {
                    System.out.println("DEBUG: ✗ Skipping appointment ID=" + apt.getId() + " - Status=" + status + " (not CONFIRMED/PENDING/RESCHEDULED or DateTime is null)");
                }
            }

            // Get blocked time slots for this date
            List<BlockedTimeSlot> blockedSlots = blockedTimeSlotRepository.findByProviderAndDate(provider, appointmentDate);

            Set<String> bookedSlots = new HashSet<>();
            // Use system default timezone for time extraction (should match doctor's availability timezone)
            TimeZone systemTimezone = TimeZone.getDefault();
            System.out.println("DEBUG: Using timezone for time extraction: " + systemTimezone.getID() + " (offset: " + systemTimezone.getRawOffset() / (60 * 60 * 1000) + " hours)");
            
            for (AppointmentEntity apt : bookedAppointments) {
                // Extract time in system default timezone to match the doctor's availability times
                // The appointment DateTime is stored in UTC, but we need to extract it in local time
                // to match the time slot format (HH:mm) which is timezone-agnostic
                Calendar aptCalLocal = Calendar.getInstance(systemTimezone);
                aptCalLocal.setTime(apt.getAppointmentDateTime());
                
                // Also check UTC for debugging
                Calendar aptCalUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                aptCalUTC.setTime(apt.getAppointmentDateTime());
                
                int hourLocal = aptCalLocal.get(Calendar.HOUR_OF_DAY);
                int minuteLocal = aptCalLocal.get(Calendar.MINUTE);
                int hourUTC = aptCalUTC.get(Calendar.HOUR_OF_DAY);
                int minuteUTC = aptCalUTC.get(Calendar.MINUTE);
                
                // Use local time for the booked slot (matching what user sees/selects)
                String bookedTime = String.format("%02d:%02d", hourLocal, minuteLocal);
                bookedSlots.add(bookedTime);
                
                System.out.println("DEBUG: Marking time slot as booked: " + bookedTime + " (Local: " + hourLocal + ":" + String.format("%02d", minuteLocal) + ", UTC: " + hourUTC + ":" + String.format("%02d", minuteUTC) + ", from appointment ID=" + apt.getId() + ", Status=" + apt.getStatus() + ", DateTime=" + apt.getAppointmentDateTime() + ")");
            }
            
            System.out.println("DEBUG: Total booked slots: " + bookedSlots.size() + " - " + bookedSlots);

            // Store blocked time ranges for overlap checking
            List<Calendar[]> blockedRanges = new ArrayList<>();
            for (BlockedTimeSlot blocked : blockedSlots) {
                String[] blockedStartParts = blocked.getStartTime().split(":");
                String[] blockedEndParts = blocked.getEndTime().split(":");
                int blockedStartHour = Integer.parseInt(blockedStartParts[0]);
                int blockedStartMinute = Integer.parseInt(blockedStartParts[1]);
                int blockedEndHour = Integer.parseInt(blockedEndParts[0]);
                int blockedEndMinute = Integer.parseInt(blockedEndParts[1]);

                Calendar blockedStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                blockedStart.setTime(appointmentDate);
                blockedStart.set(Calendar.HOUR_OF_DAY, blockedStartHour);
                blockedStart.set(Calendar.MINUTE, blockedStartMinute);
                blockedStart.set(Calendar.SECOND, 0);
                blockedStart.set(Calendar.MILLISECOND, 0);

                Calendar blockedEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                blockedEnd.setTime(appointmentDate);
                blockedEnd.set(Calendar.HOUR_OF_DAY, blockedEndHour);
                blockedEnd.set(Calendar.MINUTE, blockedEndMinute);
                blockedEnd.set(Calendar.SECOND, 0);
                blockedEnd.set(Calendar.MILLISECOND, 0);

                blockedRanges.add(new Calendar[]{blockedStart, blockedEnd});
            }
            
            // Helper function to check if a time slot overlaps with any blocked range
            java.util.function.Function<Calendar, Boolean> isSlotBlocked = (slotTime) -> {
                Calendar slotEnd = (Calendar) slotTime.clone();
                slotEnd.add(Calendar.MINUTE, appointmentDuration);
                
                for (Calendar[] range : blockedRanges) {
                    Calendar rangeStart = range[0];
                    Calendar rangeEnd = range[1];
                    
                    // Check if slot overlaps with blocked range
                    // Slot overlaps if: slotStart < rangeEnd AND slotEnd > rangeStart
                    if (slotTime.before(rangeEnd) && slotEnd.after(rangeStart)) {
                        return true;
                    }
                }
                return false;
            };

            List<Map<String, Object>> timeSlots = new ArrayList<>();
            Calendar slotStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            slotStart.setTime(appointmentDate);

            String[] startParts = actualStartTime.split(":");
            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);

            String[] endParts = actualEndTime.split(":");
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

                // Check if slot is booked
                boolean isBooked = bookedSlots.contains(timeString);
                
                // Check if slot overlaps with any blocked time range
                boolean isBlocked = isSlotBlocked.apply(slotStart);

                // Slot is available if it's not booked and not blocked
                boolean isAvailable = !isBooked && !isBlocked;

                Map<String, Object> slot = new HashMap<>();
                slot.put("time", timeString);
                slot.put("available", isAvailable);

                timeSlots.add(slot);
                slotStart.add(Calendar.MINUTE, appointmentDuration);
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