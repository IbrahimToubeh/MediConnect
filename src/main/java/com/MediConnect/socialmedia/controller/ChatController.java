package com.MediConnect.socialmedia.controller;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.repository.AppointmentRepository;
import com.MediConnect.Entities.AppointmentEntity;
import com.MediConnect.Entities.AppointmentStatus;
import com.MediConnect.config.JWTService;
import com.MediConnect.socialmedia.entity.ChatChannel;
import com.MediConnect.socialmedia.entity.ChatMessage;
import com.MediConnect.socialmedia.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chat REST Controller
 * 
 * Provides REST endpoints for chat functionality:
 * - GET /chat/channels - Get all chat channels for current user
 * - GET /chat/channels/{channelId}/messages - Get messages in a channel
 * - POST /chat/channels/{channelId}/messages - Send a message
 * - PUT /chat/channels/{channelId}/read - Mark messages as read
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    private final PatientRepo patientRepo;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final AppointmentRepository appointmentRepository;
    private final JWTService jwtService;
    
    /**
     * Get all chat channels for the authenticated user
     * 
     * Returns all chat channels where the current user is a participant
     * (either as patient or doctor). Channels are ordered by most recent activity.
     * 
     * Response includes:
     * - Channel ID
     * - Patient and doctor information
     * - Last message preview
     * - Unread message count
     */
    @GetMapping("/channels")
    public ResponseEntity<Map<String, Object>> getChatChannels(HttpServletRequest request) {
        try {
            // Extract JWT token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse("Authorization token required"));
            }
            
            String token = authHeader.substring(7);
            String username = jwtService.extractUserName(token);
            
            // Find user and get their channels
            List<ChatChannel> channels;
            Long currentUserId = null;
            
            // Try to find as patient first
            Patient patient = patientRepo.findByUsername(username).orElse(null);
            if (patient != null) {
                currentUserId = patient.getId();
                
                // AUTO-CREATE CHANNELS: Ensure channels exist for all confirmed appointments
                // This handles cases where channel creation failed during appointment confirmation
                try {
                    List<AppointmentEntity> confirmedAppointments = appointmentRepository.findByPatientId(patient.getId())
                        .stream()
                        .filter(apt -> apt.getStatus() == AppointmentStatus.CONFIRMED)
                        .toList();
                    
                    for (AppointmentEntity apt : confirmedAppointments) {
                        try {
                            // Create channel if it doesn't exist (getOrCreateChannel handles this)
                            chatService.createChannelForConfirmedAppointment(apt);
                            System.out.println("AUTO-CREATED channel for confirmed appointment ID: " + apt.getId());
                        } catch (Exception e) {
                            System.err.println("Failed to auto-create channel for appointment ID: " + apt.getId() + 
                                             " - " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error auto-creating channels for patient: " + e.getMessage());
                }
                
                channels = chatService.getPatientChannels(patient);
            } else {
                // Try as healthcare provider
                HealthcareProvider doctor = healthcareProviderRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
                currentUserId = doctor.getId();
                
                // AUTO-CREATE CHANNELS: Ensure channels exist for all confirmed appointments
                // This handles cases where channel creation failed during appointment confirmation
                try {
                    List<AppointmentEntity> confirmedAppointments = appointmentRepository.findByHealthcareProviderId(doctor.getId())
                        .stream()
                        .filter(apt -> apt.getStatus() == AppointmentStatus.CONFIRMED)
                        .collect(java.util.stream.Collectors.toList());
                    
                    for (AppointmentEntity apt : confirmedAppointments) {
                        try {
                            // Create channel if it doesn't exist (getOrCreateChannel handles this)
                            chatService.createChannelForConfirmedAppointment(apt);
                            System.out.println("AUTO-CREATED channel for confirmed appointment ID: " + apt.getId());
                        } catch (Exception e) {
                            System.err.println("Failed to auto-create channel for appointment ID: " + apt.getId() + 
                                             " - " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error auto-creating channels for doctor: " + e.getMessage());
                }
                
                channels = chatService.getDoctorChannels(doctor);
            }
            
            // Convert channels to maps with unread counts
            final Long finalUserId = currentUserId;
            List<Map<String, Object>> channelList = channels.stream().map(channel -> {
                Map<String, Object> channelMap = chatService.convertChannelToMap(channel);
                
                // Add unread count
                if (finalUserId != null) {
                    try {
                        Long unreadCount = chatService.countUnreadMessages(channel.getId(), finalUserId);
                        channelMap.put("unreadCount", unreadCount);
                    } catch (Exception e) {
                        channelMap.put("unreadCount", 0L);
                    }
                } else {
                    channelMap.put("unreadCount", 0L);
                }
                
                return channelMap;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(successResponse("data", channelList));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all messages in a chat channel
     * 
     * Returns the complete message history for a channel.
     * Messages are ordered chronologically (oldest first).
     */
    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(@PathVariable Long channelId) {
        try {
            List<ChatMessage> messages = chatService.getMessages(channelId);
            
            List<Map<String, Object>> messageList = messages.stream()
                .map(chatService::convertMessageToMap)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(successResponse("data", messageList));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Send a message in a chat channel
     * 
     * Request body: { "content": "Message text" }
     */
    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long channelId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            // Extract JWT token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse("Authorization token required"));
            }
            
            String token = authHeader.substring(7);
            String username;
            try {
                username = jwtService.extractUserName(token);
            } catch (Exception e) {
                System.err.println("Error extracting username from token: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse("Invalid token: " + e.getMessage()));
            }
            
            System.out.println("DEBUG CHAT SEND: Looking up user with username: '" + username + "'");
            
            // Find sender (patient or doctor)
            Users sender = patientRepo.findByUsername(username).orElse(null);
            if (sender == null) {
                sender = healthcareProviderRepo.findByUsername(username).orElse(null);
            }
            
            if (sender == null) {
                System.err.println("DEBUG CHAT SEND: User not found for username: " + username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse("User not found for username: " + username));
            }
            
            System.out.println("DEBUG CHAT SEND: Found sender - ID: " + sender.getId() + ", Type: " + sender.getClass().getSimpleName());
            
            // Get message content
            String content = request.get("content") != null ? request.get("content").toString() : "";
            if (content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(errorResponse("Message content cannot be empty"));
            }
            
            System.out.println("DEBUG CHAT SEND: Sending message in channel " + channelId + ", content length: " + content.length());
            
            // Send message
            ChatMessage message = chatService.sendMessage(channelId, sender, content);
            
            System.out.println("DEBUG CHAT SEND: Message saved successfully - ID: " + message.getId());
            
            return ResponseEntity.ok(successResponse("data", chatService.convertMessageToMap(message)));
            
        } catch (Exception e) {
            System.err.println("ERROR CHAT SEND: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("Failed to send message: " + e.getMessage()));
        }
    }
    
    /**
     * Mark messages as read in a chat channel
     * 
     * Called when a user opens a chat to mark all messages as read.
     */
    @PutMapping("/channels/{channelId}/read")
    public ResponseEntity<Map<String, Object>> markMessagesAsRead(
            @PathVariable Long channelId,
            HttpServletRequest request) {
        try {
            // Extract JWT token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse("Authorization token required"));
            }
            
            String token = authHeader.substring(7);
            String username = jwtService.extractUserName(token);
            
            // Find user ID
            Long userId = null;
            Patient patient = patientRepo.findByUsername(username).orElse(null);
            if (patient != null) {
                userId = patient.getId();
            } else {
                HealthcareProvider doctor = healthcareProviderRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
                userId = doctor.getId();
            }
            
            // Mark messages as read
            chatService.markMessagesAsRead(channelId, userId);
            
            return ResponseEntity.ok(successResponse("message", "Messages marked as read"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(e.getMessage()));
        }
    }
    
    // Helper methods for responses
    private Map<String, Object> successResponse(String key, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put(key, data);
        return response;
    }
    
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return error;
    }
}

