package com.MediConnect.EntryRelated.dto.appointment;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BookAppointmentRequestDTO {
    private Long doctorId;
    private String appointmentDateTime; // ISO date-time string
    private String description;
    private Boolean shareMedicalRecords = false;
    private Boolean isVideoCall = false; // Video call option (only for psychiatry doctors)
}

