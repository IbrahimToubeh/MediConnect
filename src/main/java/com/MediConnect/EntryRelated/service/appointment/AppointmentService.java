package com.MediConnect.EntryRelated.service.appointment;

import java.util.Map;

/**
 * Appointment Service Interface
 * * This service handles the complete appointment booking and management workflow.
 * It integrates with the notification system to automatically notify users about appointment changes.
 * * Appointment Workflow:
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
    Map<String, Object> updateAppointmentStatus(String token, Integer appointmentId, String status, String note, String newDateTime);

    /**
     * Patient responds to a reschedule request from doctor (accepts or rejects new time).
     * Automatically notifies the doctor about the patient's response.
     */
    Map<String, Object> respondToReschedule(String token, Integer appointmentId, String action);

    /**
     * Doctor completes an appointment after the patient visit.
     * Marks appointment as COMPLETED, adds notes, and optionally creates a follow-up appointment.
     * Automatically notifies the patient about the completion.
     *
     * @param token JWT authentication token
     * @param appointmentId The appointment to complete
     * @param notes Doctor's notes about the visit
     * @param followUpDateTime Optional: date/time for a follow-up appointment (ISO format string)
     * @return Response with updated appointment info
     */
    Map<String, Object> completeAppointment(String token, Integer appointmentId, String notes, String followUpDateTime);

    /**
     * Start video call for an appointment.
     * Sets isCallActive flag to true so patient can join.
     */
    Map<String, Object> startCall(String token, Integer appointmentId);

    /**
     * End video call for an appointment.
     * Sets isCallActive flag to false.
     */
    Map<String, Object> endCall(String token, Integer appointmentId);

    /**
     * Get available time slots for a doctor on a specific date.
     * Returns time slots with availability status based on confirmed appointments.
     * * @param doctorId The doctor's ID
     * @param date The date to check availability for (ISO date string: YYYY-MM-DD)
     * @param startTime Doctor's available start time (HH:mm format)
     * @param endTime Doctor's available end time (HH:mm format)
     * @return Map with status and list of time slots with availability
     */
    Map<String, Object> getAvailableTimeSlots(Long doctorId, String date, String startTime, String endTime);
}