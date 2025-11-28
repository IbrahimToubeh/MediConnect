package com.MediConnect.socialmedia.service;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.Entities.AppointmentEntity;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.socialmedia.entity.ChatChannel;
import com.MediConnect.socialmedia.entity.ChatMessage;
import com.MediConnect.socialmedia.repository.ChatChannelRepository;
import com.MediConnect.socialmedia.repository.ChatMessageRepository;
import com.MediConnect.socialmedia.service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chat Service
 * 
 * Handles all chat-related business logic:
 * - Creating chat channels when appointments are confirmed
 * - Ensuring only one channel exists per patient-doctor pair
 * - Sending and retrieving messages
 * - Managing read status
 */
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatChannelRepository channelRepository;
    private final ChatMessageRepository messageRepository;
    private final PatientRepo patientRepo;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Create or get existing chat channel for a patient-doctor pair
     * 
     * This method ensures only ONE channel exists per patient-doctor pair.
     * If a channel already exists, it returns that channel instead of creating a new one.
     * 
     * Called when:
     * - An appointment is confirmed (automatically creates chat)
     * - User accesses chat page (ensures channel exists for confirmed appointments)
     * 
     * @param patient The patient in the chat
     * @param doctor The doctor in the chat
     * @param appointment The appointment that triggers this channel (required for database constraint)
     * @return The chat channel (existing or newly created)
     */
    @Transactional
    public ChatChannel getOrCreateChannel(Patient patient, HealthcareProvider doctor, AppointmentEntity appointment) {
        // Check if channel already exists - handle duplicates by getting the most recent one
        // There might be duplicate channels due to earlier code issues, so we get all and pick the most recent
        List<ChatChannel> existingChannels = channelRepository.findByPatientAndDoctorOrderByLastActivityAtDesc(patient, doctor);
        
        if (!existingChannels.isEmpty()) {
            // Return the most recently active channel (first in the list since it's sorted by lastActivityAt DESC)
            ChatChannel existingChannel = existingChannels.get(0);
            System.out.println("Found existing channel ID: " + existingChannel.getId() + 
                             " between patient " + patient.getId() + 
                             " and doctor " + doctor.getId());
            return existingChannel;
        }
        
        // Channel doesn't exist, create a new one
        ChatChannel channel = new ChatChannel();
        channel.setPatient(patient);
        channel.setDoctor(doctor);
        channel.setAppointment(appointment); // Set appointment to satisfy database NOT NULL constraint
        channel.setIsActive(true);
        channel.setCreatedAt(new Date());
        channel.setLastActivityAt(new Date());
        
        ChatChannel saved = channelRepository.save(channel);
        System.out.println("Created chat channel between patient " + patient.getId() + 
                         " and doctor " + doctor.getId() + 
                         " for appointment " + appointment.getId());
        return saved;
    }
    
    /**
     * Create chat channel when appointment is confirmed
     * 
     * This is called automatically when a doctor confirms an appointment.
     * Creates a chat channel so patient and doctor can communicate.
     * This works for ALL confirmed appointments, including regular and video call appointments.
     * 
     * @param appointment The confirmed appointment
     */
    @Transactional
    public void createChannelForConfirmedAppointment(AppointmentEntity appointment) {
        try {
            System.out.println("DEBUG CHAT SERVICE: createChannelForConfirmedAppointment called");
            System.out.println("DEBUG CHAT SERVICE: Appointment ID: " + appointment.getId());
            System.out.println("DEBUG CHAT SERVICE: Appointment status: " + (appointment.getStatus() != null ? appointment.getStatus().name() : "NULL"));
            
            // Only create channel if appointment is confirmed
            if (appointment.getStatus() == null || 
                !appointment.getStatus().equals(com.MediConnect.Entities.AppointmentStatus.CONFIRMED)) {
                System.out.println("DEBUG CHAT SERVICE: Skipping channel creation - appointment status is not CONFIRMED");
                return;
            }
            
            Patient patient = appointment.getPatient();
            HealthcareProvider doctor = appointment.getHealthcareProvider();
            
            if (patient == null || doctor == null) {
                System.err.println("ERROR CHAT SERVICE: Patient or Doctor is null - Patient: " + 
                    (patient != null ? patient.getId() : "NULL") + 
                    ", Doctor: " + (doctor != null ? doctor.getId() : "NULL"));
                return;
            }
            
            System.out.println("DEBUG CHAT SERVICE: Creating channel - Patient ID: " + patient.getId() + 
                             ", Doctor ID: " + doctor.getId() + 
                             ", Appointment ID: " + appointment.getId());
            
            // Create or get existing channel (ensures only one channel per patient-doctor pair)
            // Pass the appointment so it can be stored in the channel (required by database constraint)
            ChatChannel channel = getOrCreateChannel(patient, doctor, appointment);
            
            System.out.println("DEBUG CHAT SERVICE: Channel created/retrieved successfully - Channel ID: " + channel.getId());
        } catch (Exception e) {
            System.err.println("ERROR CHAT SERVICE: Exception in createChannelForConfirmedAppointment: " + 
                             e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to allow AppointmentServiceImpl to handle it
        }
    }
    
    /**
     * Send a message in a chat channel
     * 
     * @param channelId The channel to send message in
     * @param sender The user sending the message (patient or doctor)
     * @param content The message content
     * @return The saved message
     */
    @Transactional
    public ChatMessage sendMessage(Long channelId, Users sender, String content) {
        try {
            // Find the channel
            ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Chat channel not found with ID: " + channelId));
            
            System.out.println("DEBUG CHAT SERVICE: Channel found - ID: " + channel.getId() + 
                              ", Patient: " + channel.getPatient().getId() + 
                              ", Doctor: " + channel.getDoctor().getId());
            
            // Validate sender is either the patient or doctor in this channel
            Long senderId = sender.getId();
            if (!senderId.equals(channel.getPatient().getId()) && 
                !senderId.equals(channel.getDoctor().getId())) {
                throw new RuntimeException("User " + senderId + " is not authorized to send messages in this channel");
            }
            
            System.out.println("DEBUG CHAT SERVICE: Creating message - Channel: " + channelId + 
                              ", Sender ID: " + senderId + ", Sender Type: " + sender.getClass().getSimpleName() + 
                              ", Content: " + (content.length() > 50 ? content.substring(0, 50) + "..." : content));
            
            // Ensure sender is a managed entity (important for JOINED inheritance)
            // If sender is detached, re-fetch it from the appropriate repository
            Users managedSender = sender;
            if (!entityManager.contains(sender)) {
                System.out.println("DEBUG CHAT SERVICE: Sender is detached, re-fetching...");
                // Re-fetch based on sender's actual type
                if (sender instanceof Patient) {
                    managedSender = patientRepo.findById(sender.getId())
                        .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + sender.getId()));
                } else if (sender instanceof HealthcareProvider) {
                    managedSender = healthcareProviderRepo.findById(sender.getId())
                        .orElseThrow(() -> new RuntimeException("HealthcareProvider not found with ID: " + sender.getId()));
                } else {
                    // If it's just Users, we need to determine type from channel
                    if (senderId.equals(channel.getPatient().getId())) {
                        managedSender = patientRepo.findById(senderId)
                            .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + senderId));
                    } else {
                        managedSender = healthcareProviderRepo.findById(senderId)
                            .orElseThrow(() -> new RuntimeException("HealthcareProvider not found with ID: " + senderId));
                    }
                }
                System.out.println("DEBUG CHAT SERVICE: Re-fetched sender - Type: " + managedSender.getClass().getSimpleName());
            }
            
            // Create message
            ChatMessage message = new ChatMessage();
            message.setChannel(channel);
            message.setSender(managedSender); // Use managed entity
            message.setContent(content);
            message.setSentAt(new Date());
            message.setIsRead(false);
            message.setIsDeleted(false); // Ensure isDeleted is set to false
            
            // Update channel's last activity time
            channel.setLastActivityAt(new Date());
            channelRepository.save(channel);
            
            System.out.println("DEBUG CHAT SERVICE: Attempting to save message...");
            
            // Save message
            ChatMessage savedMessage = messageRepository.save(message);
            
            System.out.println("DEBUG CHAT SERVICE: Message saved successfully - ID: " + savedMessage.getId());
            
            // BROADCAST: Send message to WebSocket subscribers
            try {
                Map<String, Object> messageMap = convertMessageToMap(savedMessage);
                messagingTemplate.convertAndSend("/topic/chat/" + channelId, messageMap);
                System.out.println("DEBUG CHAT SERVICE: Broadcasted message to /topic/chat/" + channelId);
            } catch (Exception e) {
                System.err.println("ERROR CHAT SERVICE: Failed to broadcast message: " + e.getMessage());
                e.printStackTrace();
            }
            
            // NOTIFICATION: Send notification to the recipient (the other person in the chat)
            // The recipient is whoever is NOT the sender (patient if doctor sent, doctor if patient sent)
            Users recipient = null;
            if (senderId.equals(channel.getPatient().getId())) {
                // Patient sent the message, so doctor is the recipient
                recipient = channel.getDoctor();
            } else {
                // Doctor sent the message, so patient is the recipient
                recipient = channel.getPatient();
            }
            
            // NOTIFICATION: Create notification for the recipient (the other person in the chat)
            // This runs in a separate try-catch to ensure message sending succeeds even if notification fails
            if (recipient != null) {
                try {
                    notificationService.createChatMessageNotification(
                        managedSender,  // sender (actor)
                        recipient,      // recipient (the other person in chat)
                        channelId,      // channel ID for navigation
                        content         // message preview
                    );
                    System.out.println("DEBUG CHAT SERVICE: Notification created successfully for recipient ID: " + recipient.getId());
                } catch (Exception e) {
                    // Log but don't fail message sending if notification fails
                    // This ensures users can still send messages even if there's a notification issue
                    System.err.println("ERROR CHAT SERVICE: Failed to create notification (message still sent): " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            return savedMessage;
        } catch (Exception e) {
            System.err.println("ERROR CHAT SERVICE sendMessage: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Get all messages in a channel.
     * This is a read-only query operation.
     * 
     * @param channelId The channel ID
     * @return List of messages ordered by sent time (oldest first)
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Long channelId) {
        return messageRepository.findByChannelIdOrderBySentAtAsc(channelId);
    }
    
    /**
     * Get all chat channels for a patient.
     * Returns only ONE channel per doctor (handles duplicates by keeping the most recently active one).
     * This is a read-only query operation.
     * 
     * @param patient The patient
     * @return List of channels ordered by most recent activity (one per doctor)
     */
    @Transactional(readOnly = true)
    public List<ChatChannel> getPatientChannels(Patient patient) {
        List<ChatChannel> allChannels = channelRepository.findByPatientOrderByLastActivityAtDesc(patient);
        
        // Group by doctor and keep only the most recently active channel per doctor
        Map<Long, ChatChannel> channelMap = new HashMap<>();
        for (ChatChannel channel : allChannels) {
            Long doctorId = channel.getDoctor().getId();
            
            // If we haven't seen this doctor yet, or this channel is more recent, keep it
            if (!channelMap.containsKey(doctorId) || 
                channel.getLastActivityAt().after(channelMap.get(doctorId).getLastActivityAt())) {
                channelMap.put(doctorId, channel);
            }
        }
        
        // Return channels sorted by last activity (most recent first)
        return channelMap.values().stream()
            .sorted((c1, c2) -> c2.getLastActivityAt().compareTo(c1.getLastActivityAt()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get all chat channels for a doctor.
     * Returns only ONE channel per patient (handles duplicates by keeping the most recently active one).
     * This is a read-only query operation.
     * 
     * @param doctor The doctor
     * @return List of channels ordered by most recent activity (one per patient)
     */
    @Transactional(readOnly = true)
    public List<ChatChannel> getDoctorChannels(HealthcareProvider doctor) {
        List<ChatChannel> allChannels = channelRepository.findByDoctorOrderByLastActivityAtDesc(doctor);
        
        // Group by patient and keep only the most recently active channel per patient
        Map<Long, ChatChannel> channelMap = new HashMap<>();
        for (ChatChannel channel : allChannels) {
            Long patientId = channel.getPatient().getId();
            
            // If we haven't seen this patient yet, or this channel is more recent, keep it
            if (!channelMap.containsKey(patientId) || 
                channel.getLastActivityAt().after(channelMap.get(patientId).getLastActivityAt())) {
                channelMap.put(patientId, channel);
            }
        }
        
        // Return channels sorted by last activity (most recent first)
        return channelMap.values().stream()
            .sorted((c1, c2) -> c2.getLastActivityAt().compareTo(c1.getLastActivityAt()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Mark messages as read in a channel
     * 
     * @param channelId The channel ID
     * @param userId The user who is reading (to exclude their own messages)
     */
    @Transactional
    public void markMessagesAsRead(Long channelId, Long userId) {
        messageRepository.markMessagesAsRead(channelId, userId);
    }
    
    /**
     * Count unread messages in a channel.
     * This is a read-only query operation.
     * 
     * @param channelId The channel ID
     * @param userId The user checking unread count
     * @return Number of unread messages
     */
    @Transactional(readOnly = true)
    public Long countUnreadMessages(Long channelId, Long userId) {
        return messageRepository.countUnreadMessages(channelId, userId);
    }
    
    /**
     * Convert ChatChannel entity to Map for API response
     * 
     * @param channel The channel entity
     * @return Map with channel data
     */
    public Map<String, Object> convertChannelToMap(ChatChannel channel) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", channel.getId());
        
        // Patient info
        Map<String, Object> patientInfo = new HashMap<>();
        patientInfo.put("id", channel.getPatient().getId());
        patientInfo.put("firstName", channel.getPatient().getFirstName());
        patientInfo.put("lastName", channel.getPatient().getLastName());
        patientInfo.put("profilePicture", channel.getPatient().getProfilePicture());
        map.put("patient", patientInfo);
        
        // Doctor info
        Map<String, Object> doctorInfo = new HashMap<>();
        doctorInfo.put("id", channel.getDoctor().getId());
        doctorInfo.put("firstName", channel.getDoctor().getFirstName());
        doctorInfo.put("lastName", channel.getDoctor().getLastName());
        doctorInfo.put("specialty", channel.getDoctor().getSpecializations() != null && 
                      !channel.getDoctor().getSpecializations().isEmpty() ?
                      channel.getDoctor().getSpecializations().get(0).name() : "");
        doctorInfo.put("profilePicture", channel.getDoctor().getProfilePicture());
        map.put("doctor", doctorInfo);
        
        // Last message preview
        ChatMessage lastMessage = messageRepository.findFirstByChannelIdOrderBySentAtDesc(channel.getId());
        if (lastMessage != null) {
            Map<String, Object> lastMsg = new HashMap<>();
            lastMsg.put("id", lastMessage.getId());
            lastMsg.put("content", lastMessage.getContent());
            lastMsg.put("sentAt", lastMessage.getSentAt().toInstant().toString());
            lastMsg.put("senderId", lastMessage.getSender().getId());
            map.put("lastMessage", lastMsg);
        }
        
        map.put("lastActivityAt", channel.getLastActivityAt().toInstant().toString());
        map.put("createdAt", channel.getCreatedAt().toInstant().toString());
        
        return map;
    }
    
    /**
     * Convert ChatMessage entity to Map for API response
     * 
     * Includes sender information (name, profile picture) so frontend can display who sent each message
     * 
     * @param message The message entity
     * @return Map with message data including sender details
     */
    public Map<String, Object> convertMessageToMap(ChatMessage message) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", message.getId());
        map.put("channelId", message.getChannel().getId());
        map.put("content", message.getContent());
        map.put("sentAt", message.getSentAt().toInstant().toString());
        map.put("senderId", message.getSender().getId());
        map.put("isRead", message.getIsRead());
        if (message.getReadAt() != null) {
            map.put("readAt", message.getReadAt().toInstant().toString());
        }
        
        // Add sender information so frontend can display who sent the message
        Map<String, Object> senderMap = new HashMap<>();
        senderMap.put("id", message.getSender().getId());
        senderMap.put("firstName", message.getSender().getFirstName());
        senderMap.put("lastName", message.getSender().getLastName());
        senderMap.put("profilePicture", message.getSender().getProfilePicture());
        
        // Determine if sender is a doctor (HealthcareProvider) or patient
        boolean isDoctor = message.getSender() instanceof HealthcareProvider;
        senderMap.put("isDoctor", isDoctor);
        
        map.put("sender", senderMap);
        
        return map;
    }
}

