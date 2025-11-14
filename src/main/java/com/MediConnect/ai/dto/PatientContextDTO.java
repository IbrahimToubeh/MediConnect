package com.MediConnect.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Snapshot of the structured information the chatbot has gathered so far.
 * <p>
 * The fields intentionally mirror the questions our assistant should ask.
 * Every field is optional; the chatbot gradually fills them as the conversation progresses.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientContextDTO {
    private String ageRange;
    private String primaryConcern;
    private String symptomDuration;
    private String symptomSeverity;
    private String additionalSymptoms;
    private String medicalHistory;
    private String medications;
    private String allergies;
    private String preferredDoctorGender;
    private String preferredLanguage;
    private String insuranceProvider;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String preferredSpecialization;
    private String appointmentPreference; // e.g. in-person vs virtual
}

