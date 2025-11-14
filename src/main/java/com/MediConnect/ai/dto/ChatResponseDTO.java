package com.MediConnect.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Response returned to the frontend after every chatbot interaction.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponseDTO {
    /**
     * Natural language message that should be rendered to the patient.
     */
    private String reply;

    /**
     * Updated snapshot of the structured data the assistant has collected so far.
     */
    private PatientContextDTO context;

    /**
     * Doctors that match the patient profile according to the AI.
     */
    private List<DoctorSuggestionDTO> recommendedDoctors = new ArrayList<>();

    /**
     * Optional list of follow-up prompts the UI may surface as quick replies.
     */
    private List<String> followUpQuestions = new ArrayList<>();

    /**
     * Optional navigation tips that guide the patient through MediConnect features.
     */
    private List<String> navigationTips = new ArrayList<>();

    /**
     * Flags whether the assistant believes it has enough information to make a recommendation.
     */
    private boolean informationComplete;

    /**
     * Raw content returned by the language model (useful for debug logging on the frontend).
     */
    private String rawModelContent;
}

