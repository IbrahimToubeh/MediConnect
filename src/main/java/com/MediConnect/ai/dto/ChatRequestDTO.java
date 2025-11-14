package com.MediConnect.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload sent from the frontend whenever the patient submits a new message.
 */
@Data
public class ChatRequestDTO {

    @NotEmpty(message = "messages are required")
    @Valid
    private List<ChatMessageDTO> messages = new ArrayList<>();

    /**
     * Structured information collected so far.
     * The assistant updates this object on every turn and the frontend echoes it back.
     */
    @Valid
    private PatientContextDTO context;
}

