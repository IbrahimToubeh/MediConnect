package com.MediConnect.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * Represents a single entry in the running conversation between the patient and the AI assistant.
 * <p>
 * Only two roles are expected: {@code user} (the patient) and {@code assistant} (the AI).
 * We normalise the role to lower-case so the OpenAI API receives consistent payloads even if
 * the frontend accidentally sends a capitalised value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    @NotBlank(message = "role is required")
    private String role;

    @NotBlank(message = "content is required")
    private String content;

    public void setRole(String role) {
        this.role = normaliseRole(role);
    }

    @JsonIgnore
    public String getNormalisedRole() {
        return normaliseRole(role);
    }

    private String normaliseRole(String value) {
        String cleaned = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return cleaned.isEmpty() ? "user" : cleaned;
    }
}

