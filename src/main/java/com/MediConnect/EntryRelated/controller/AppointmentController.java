package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.service.appointment.AppointmentService;
import com.MediConnect.EntryRelated.service.appointment.impl.AppointmentServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Appointment REST Controller
 *
 * Endpoints for appointment management:
 * - POST /appointments/book - Patient books an appointment (triggers notification to doctor)
 * - GET /appointments/patient - Get patient's appointments
 * - GET /appointments/doctor - Get doctor's appointments (includes insurance & medical records if shared)
 * - PUT /appointments/{id}/status - Doctor updates appointment status (triggers notification to patient)
 * - PUT /appointments/{id}/respond-reschedule - Patient responds to reschedule (triggers notification to doctor)
 */
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Endpoint for patients to book appointments.
     * Requires: Authorization header with Bearer token
     * Request body: { doctorId, appointmentDateTime, description, shareMedicalRecords }
     */
    @PostMapping("/book")
    public ResponseEntity<Map<String, Object>> bookAppointment(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Authorization token required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            Map<String, Object> response = appointmentService.bookAppointment(authHeader, request);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/patient")
    public ResponseEntity<Map<String, Object>> getPatientAppointments(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Authorization token required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            Map<String, Object> response = appointmentService.getPatientAppointments(authHeader);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/doctor")
    public ResponseEntity<Map<String, Object>> getDoctorAppointments(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Authorization token required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            Map<String, Object> response = appointmentService.getDoctorAppointments(authHeader);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get available time slots for a doctor on a specific date.
     * Checks confirmed appointments and returns slots with availability status.
     *
     * Query parameters:
     * - doctorId: The doctor's ID
     * - date: The date to check (YYYY-MM-DD format)
     * - startTime: Doctor's available start time (HH:mm format)
     * - endTime: Doctor's available end time (HH:mm format)
     *
     * Returns: List of time slots with availability status
     */
    @GetMapping("/available-slots")
    public ResponseEntity<Map<String, Object>> getAvailableTimeSlots(
            @RequestParam("doctorId") Long doctorId,
            @RequestParam("date") String date,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime) {
        try {
            Map<String, Object> response = appointmentService.getAvailableTimeSlots(doctorId, date, startTime, endTime);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Endpoint for doctors to update appointment status (confirm, cancel, or reschedule).
     * When called, automatically sends notification to the patient.
     *
     * Status values: "CONFIRMED", "CANCELLED", or "RESCHEDULED"
     * If status is "RESCHEDULED", newAppointmentDateTime must be provided with the new time.
     *
     * Request body: { status, doctorNotes (optional), newAppointmentDateTime (required if rescheduling) }
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateAppointmentStatus(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Authorization token required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            // Extract status and optional parameters
            // newAppointmentDateTime is required when status is "RESCHEDULED"
            String status = body.get("status") != null ? body.get("status").toString() : "";
            String note = body.get("doctorNotes") != null ? body.get("doctorNotes").toString() :
                         (body.get("note") != null ? body.get("note").toString() : null);
            String newDateTime = body.get("newAppointmentDateTime") != null ? body.get("newAppointmentDateTime").toString() : null;

            Map<String, Object> response = appointmentService.updateAppointmentStatus(authHeader, id, status, note, newDateTime);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Endpoint for patients to respond to a reschedule request from the doctor.
     * When called, automatically sends notification to the doctor about the response.
     *
     * Action values: "confirm" (accept new time) or "cancel" (reject and cancel appointment)
     * Only works if appointment status is currently RESCHEDULED.
     */
    @PutMapping("/{id}/respond-reschedule")
    public ResponseEntity<Map<String, Object>> respondToReschedule(
            @PathVariable("id") Integer id,
            @RequestParam("action") String action,
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Authorization token required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            Map<String, Object> response = appointmentService.respondToReschedule(authHeader, id, action);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Endpoint for doctors to complete an appointment after the patient visit.
     *
     * Marks the appointment as COMPLETED, adds notes, and optionally creates a follow-up appointment.
     * Only works for appointments with status CONFIRMED (upcoming appointments).
     *
     * Request body: { notes (optional), followUpDateTime (optional, ISO format string) }
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeAppointment(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Authorization token required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            String notes = body.get("notes") != null ? body.get("notes").toString() : null;
            String followUpDateTime = body.get("followUpDateTime") != null ? body.get("followUpDateTime").toString() : null;

            Map<String, Object> response = appointmentService.completeAppointment(authHeader, id, notes, followUpDateTime);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Start video call for an appointment
     * Only doctors can start calls for their confirmed video call appointments
     */
    @PostMapping("/{id}/start-call")
    public ResponseEntity<Map<String, Object>> startCall(
            @PathVariable("id") Integer id,
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Authorization token required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            Map<String, Object> response = appointmentService.startCall(authHeader, id);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * End video call for an appointment
     * Only doctors can end calls for their appointments
     */
    @PostMapping("/{id}/end-call")
    public ResponseEntity<Map<String, Object>> endCall(
            @PathVariable("id") Integer id,
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Authorization token required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            Map<String, Object> response = appointmentService.endCall(authHeader, id);
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

