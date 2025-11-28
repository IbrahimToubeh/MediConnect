package com.MediConnect.socialmedia.chatrelated;

import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.socialmedia.entity.ChatMessage;
import com.MediConnect.socialmedia.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final PatientRepo patientRepo;
    private final HealthcareProviderRepo providerRepo;

    @MessageMapping("/chat/{channelId}/sendMessage")
    @SendTo("/topic/chat/{channelId}")
    public Map<String, Object> sendMessage(
            @DestinationVariable Long channelId,
            @Payload ChatMessageRequest request,
            Principal principal
    ) {
        try {
            String username = null;

            // 1. Try Secure Principal
            if (principal != null) {
                username = principal.getName();
            }

            // 2. Fallback to Payload if Principal is missing (Fixes connection issues)
            if (username == null && request.getSenderUsername() != null) {
                username = request.getSenderUsername();
            }

            if (username == null) throw new RuntimeException("No user identified");

            // 3. Find User in DB
            Users sender = null;
            Optional<com.MediConnect.EntryRelated.entities.Patient> patient = patientRepo.findByUsername(username);
            if (patient.isPresent()) sender = patient.get();
            else {
                Optional<com.MediConnect.EntryRelated.entities.HealthcareProvider> doctor = providerRepo.findByUsername(username);
                if (doctor.isPresent()) sender = doctor.get();
            }

            if (sender == null) throw new RuntimeException("User not found: " + username);

            // 4. Save & Broadcast
            ChatMessage savedMessage = chatService.sendMessage(channelId, sender, request.getContent());

            return chatService.convertMessageToMap(savedMessage);

        } catch (Exception e) {
            log.error("WebSocket Error: ", e);
            return null;
        }
    }
}