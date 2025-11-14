package com.MediConnect.EntryRelated.controller;

import com.MediConnect.ai.dto.ChatRequestDTO;
import com.MediConnect.ai.dto.ChatResponseDTO;
import com.MediConnect.ai.service.PatientRecommendationChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the AI chatbot endpoint consumed by the landing page and patient dashboards.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class PatientAssistantController {

    private final PatientRecommendationChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request) {
        log.debug("Received chatbot request with {} messages", request.getMessages().size());
        ChatResponseDTO response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }
}

