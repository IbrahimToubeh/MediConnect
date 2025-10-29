package com.MediConnect.EntryRelated.service.appointment;

import java.util.List;
import java.util.Map;

public interface AppointmentService {
    Map<String, Object> bookAppointment(String token, Map<String, Object> request);
    Map<String, Object> getPatientAppointments(String token);
    Map<String, Object> getDoctorAppointments(String token);
    Map<String, Object> updateAppointmentStatus(String token, Long appointmentId, String status, String note, String newDateTime);
    Map<String, Object> respondToReschedule(String token, Long appointmentId, String action);
}

