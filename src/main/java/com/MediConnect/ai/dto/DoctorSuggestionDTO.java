package com.MediConnect.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Lightweight projection of a doctor profile that is safe to show inside the chatbot.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoctorSuggestionDTO {
    Long id;
    String fullName;
    String clinicName;
    String city;
    String state;
    String country;
    Double consultationFee;
    String shortBio;
    String profilePicture;
    List<String> specializations;
    List<String> insuranceAccepted;
    Double matchScore;
}

