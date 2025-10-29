package com.MediConnect.EntryRelated.service.appointment;

import java.util.Map;

/**
 * Appointment Service Interface
 * 
 * This service handles the complete appointment booking and management workflow.
 * It integrates with the notification system to automatically notify users about appointment changes.
 * 
 * Appointment Workflow:
 * 1. Patient books appointment → Doctor receives APPOINTMENT_REQUESTED notification
 * 2. Doctor confirms/cancels/reschedules → Patient receives status change notification
 * 3. Patient responds to reschedule → Doctor receives reschedule response notification
 */
public interface AppointmentService {
    /**
     * Books a new appointment. When called, it automatically sends a notification to the doctor.
     */
    Map<String, Object> bookAppointment(String token, Map<String, Object> request);
    
    /**
     * Gets all appointments for the authenticated patient.
     * Returns appointments with doctor info, insurance info, and medical records (if shared).
     */
    Map<String, Object> getPatientAppointments(String token);
    
    /**
     * Gets all appointments for the authenticated doctor.
     * Returns appointments with patient info, insurance info (always visible), 
     * and medical records (only if patient consented to share during booking).
     */
    Map<String, Object> getDoctorAppointments(String token);
    
    /**
     * Doctor updates appointment status (CONFIRMED, CANCELLED, or RESCHEDULED).
     * Automatically notifies the patient about the status change.
     */
    Map<String, Object> updateAppointmentStatus(String token, Long appointmentId, String status, String note, String newDateTime);
    
    /**
     * Patient responds to a reschedule request from doctor (accepts or rejects new time).
     * Automatically notifies the doctor about the patient's response.
     */
    Map<String, Object> respondToReschedule(String token, Long appointmentId, String action);
}

