package com.MediConnect.socialmedia.service;

import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.service.NotificationPreferencesService;
import com.MediConnect.socialmedia.entity.*;
import com.MediConnect.socialmedia.repository.NotificationRepository;
import com.MediConnect.Repos.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing notifications.
 * Notification creation methods are asynchronous to improve response times.
 * Read-only methods (getUserNotifications, getUnreadCount) remain synchronous.
 */
@Slf4j
@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private NotificationPreferencesService notificationPreferencesService;

    /**
     * Retrieves all admin users from the database.
     * Results are cached since the admin list rarely changes.
     * Cache is automatically evicted when admin users are added/removed (manual eviction required).
     * 
     * Cache key: "adminUsers"
     * Cache TTL: 30 minutes (longer than unreadCount since admin list changes very rarely)
     * 
     * @return List of admin users
     */
    @Cacheable(value = "adminUsers", key = "'all'")
    @Transactional(readOnly = true)
    public List<Users> getAdminUsers() {
        log.debug("Fetching admin users from database (cache miss)");
        return userRepo.findByRoleIgnoreCase("ADMIN");
    }

    /**
     * Creates a notification addressed to every administrator in the system.
     * This method runs asynchronously to avoid blocking the main request thread.
     *
     * @param actor            the user that triggered the notification (cannot be {@code null})
     * @param message          human-readable summary displayed in the notification center
     * @param notificationType type identifier stored with the notification
     * @param relatedEntityId  optional id (post id, registration id, etc.) used for navigation
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createAdminNotification(Users actor,
                                        String message,
                                        NotificationType notificationType,
                                        Long relatedEntityId) {
        if (actor == null) {
            throw new IllegalArgumentException("Actor is required when creating admin notifications.");
        }
        if (notificationType == null) {
            throw new IllegalArgumentException("Notification type must be provided.");
        }
        // Admin list is cached to reduce database queries (admin list rarely changes)
        List<Users> admins = getAdminUsers();
        if (admins == null || admins.isEmpty()) {
            return;
        }

        for (Users admin : admins) {
            if (admin.getId().equals(actor.getId())) {
                continue;
            }
            Notification notification = new Notification();
            notification.setRecipient(admin);
            notification.setActor(actor);
            notification.setType(notificationType);
            notification.setRelatedEntityId(relatedEntityId);
            notification.setMessage(message);
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            // Evict unread count cache for the admin since a new notification was created
            evictUnreadCountCache(admin.getId());
        }
    }
    
    /**
     * Creates a notification when someone likes a post.
     * This method runs asynchronously to avoid blocking the main request thread.
     * 
     * @param actor The user who liked the post
     * @param post The post that was liked
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createPostLikeNotification(Users actor, MedicalPost post) {
        // Don't notify if the actor is liking their own post
        if (post.getPostProvider().getId().equals(actor.getId())) {
            return;
        }
        
        // Check if the recipient has post likes notifications enabled
        if (!notificationPreferencesService.isNotificationEnabled(post.getPostProvider(), "post_likes")) {
            return;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(post.getPostProvider());
        notification.setActor(actor);
        notification.setType(NotificationType.POST_LIKE);
        notification.setPost(post);
        notification.setRelatedEntityId(post.getId());
        notification.setMessage(actor.getFirstName() + " " + actor.getLastName() + " liked your post");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Evict unread count cache for the recipient since a new notification was created
        evictUnreadCountCache(post.getPostProvider().getId());
    }
    
    /**
     * Creates a notification when someone comments on a post.
     * This method runs asynchronously to avoid blocking the main request thread.
     * 
     * @param actor The user who commented
     * @param post The post that was commented on
     * @param comment The comment that was created
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createPostCommentNotification(Users actor, MedicalPost post, MedicalPostComment comment) {
        // Don't notify if the actor is commenting on their own post
        if (post.getPostProvider().getId().equals(actor.getId())) {
            return;
        }
        
        // Check if the recipient has post comments notifications enabled
        if (!notificationPreferencesService.isNotificationEnabled(post.getPostProvider(), "post_comments")) {
            return;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(post.getPostProvider());
        notification.setActor(actor);
        notification.setType(NotificationType.POST_COMMENT);
        notification.setPost(post);
        notification.setComment(comment);
        notification.setRelatedEntityId(post.getId());
        notification.setMessage(actor.getFirstName() + " " + actor.getLastName() + " commented on your post");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Evict unread count cache for the recipient since a new notification was created
        evictUnreadCountCache(post.getPostProvider().getId());
    }
    
    /**
     * Creates a notification when someone likes a comment.
     * This method runs asynchronously to avoid blocking the main request thread.
     * 
     * @param actor The user who liked the comment
     * @param comment The comment that was liked
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createCommentLikeNotification(Users actor, MedicalPostComment comment) {
        log.debug("Creating comment like notification - Actor: {} (ID: {}), Comment ID: {}, Commenter ID: {}", 
                actor.getUsername(), actor.getId(), comment.getId(), comment.getCommenterId());
        
        // Don't notify if the actor is liking their own comment
        Users commentOwner = userRepo.findById(comment.getCommenterId()).orElse(null);
        if (commentOwner == null) {
            log.warn("Comment owner not found for comment ID: {}", comment.getId());
            return;
        }
        
        log.debug("Comment owner: {} (ID: {})", commentOwner.getUsername(), commentOwner.getId());
        
        if (commentOwner.getId().equals(actor.getId())) {
            log.debug("Actor is liking own comment - no notification");
            return;
        }
        
        // Check if the recipient has comment likes notifications enabled (using post_likes for now)
        boolean notificationsEnabled = notificationPreferencesService.isNotificationEnabled(commentOwner, "post_likes");
        log.debug("Notifications enabled for user: {}", notificationsEnabled);
        
        if (!notificationsEnabled) {
            log.debug("Notifications disabled for this user - no notification created");
            return;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(commentOwner);
        notification.setActor(actor);
        notification.setType(NotificationType.COMMENT_LIKE);
        notification.setComment(comment);
        notification.setPost(comment.getPost());
        notification.setRelatedEntityId(comment.getPost().getId());
        notification.setMessage(actor.getFirstName() + " " + actor.getLastName() + " liked your comment");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Evict unread count cache for the recipient since a new notification was created
        evictUnreadCountCache(commentOwner.getId());
        log.debug("Comment like notification created successfully for comment ID: {}", comment.getId());
    }
    
    /**
     * Creates a notification when someone replies to a comment.
     * This method runs asynchronously to avoid blocking the main request thread.
     * 
     * @param actor The user who replied
     * @param reply The reply that was created
     * @param parentComment The parent comment that was replied to
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createCommentReplyNotification(Users actor, CommentReply reply, MedicalPostComment parentComment) {
        // Don't notify if the actor is replying to their own comment
        Users commentOwner = userRepo.findById(parentComment.getCommenterId()).orElse(null);
        if (commentOwner == null || commentOwner.getId().equals(actor.getId())) {
            return;
        }
        
        // Check if the recipient has comment replies notifications enabled
        if (!notificationPreferencesService.isNotificationEnabled(commentOwner, "comment_replies")) {
            return;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(commentOwner);
        notification.setActor(actor);
        notification.setType(NotificationType.COMMENT_REPLY);
        notification.setComment(parentComment);
        notification.setPost(parentComment.getPost());
        notification.setRelatedEntityId(parentComment.getPost().getId());
        notification.setMessage(actor.getFirstName() + " " + actor.getLastName() + " replied to your comment");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Evict unread count cache for the recipient since a new notification was created
        evictUnreadCountCache(commentOwner.getId());
    }
    
    /**
     * Retrieves all notifications for a specific user.
     * This is a read-only query operation.
     * 
     * @param userId The ID of the user
     * @return List of notification maps ordered by creation date (newest first)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserNotifications(Long userId) {
        Users user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
        
        return notifications.stream()
            .map(this::convertToMap)
            .collect(Collectors.toList());
    }
    
    /**
     * Counts unread notifications for a specific user.
     * This is a read-only query operation.
     * 
     * Results are cached to reduce database load, as this method is called frequently
     * (e.g., when displaying notification bell badge). Cache is automatically invalidated
     * when notifications are marked as read via markAsRead() or markAllAsRead().
     * 
     * Cache key: "unreadCount:{userId}"
     * Cache TTL: 5 minutes (configured in RedisConfig)
     * 
     * @param userId The ID of the user
     * @return The count of unread notifications
     */
    /**
     * Counts unread notifications for a specific user.
     * This is a read-only query operation.
     * 
     * Results are cached to reduce database load, as this method is called frequently
     * (e.g., when displaying notification bell badge). Cache is automatically invalidated
     * when notifications are marked as read via markAsRead() or markAllAsRead().
     * 
     * Cache key: "unreadCount:{userId}"
     * Cache TTL: 5 minutes (configured in RedisConfig)
     * 
     * Note: This method handles type conversion to ensure Long is always returned,
     * as Redis may deserialize small numbers as Integer.
     * 
     * @param userId The ID of the user
     * @return The count of unread notifications (always Long)
     */
    @Cacheable(value = "unreadCount", key = "#userId")
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        log.debug("Fetching unread count for user {} from database (cache miss)", userId);
        Users user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Long count = notificationRepository.countByRecipientAndIsReadFalse(user);
        log.debug("Unread count for user {}: {}", userId, count);
        // Ensure we always return Long (repository returns Long, but ensure type safety)
        return count != null ? Long.valueOf(count) : 0L;
    }
    
    /**
     * Marks a single notification as read.
     * 
     * This method invalidates the unread count cache for the notification recipient
     * to ensure the cached count is updated when notifications are read.
     * 
     * @param notificationId The ID of the notification to mark as read
     */
    @CacheEvict(value = "unreadCount", key = "#result?.recipient?.id", beforeInvocation = false)
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        log.debug("Marked notification {} as read for user {}", notificationId, notification.getRecipient().getId());
        
        // Manually evict cache since @CacheEvict with result doesn't work well with void methods
        evictUnreadCountCache(notification.getRecipient().getId());
    }
    
    /**
     * Helper method to evict unread count cache for a specific user.
     * Used when cache eviction needs to happen after the transaction completes.
     * 
     * @param userId The ID of the user whose cache should be evicted
     */
    @CacheEvict(value = "unreadCount", key = "#userId")
    public void evictUnreadCountCache(Long userId) {
        log.debug("Evicting unread count cache for user {}", userId);
    }
    
    /**
     * Marks all notifications as read for a specific user.
     * 
     * This method invalidates the unread count cache for the user to ensure
     * the cached count is updated (should be 0 after this operation).
     * 
     * @param userId The ID of the user whose notifications should be marked as read
     */
    @CacheEvict(value = "unreadCount", key = "#userId")
    @Transactional
    public void markAllAsRead(Long userId) {
        Users user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(user);
        
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
        log.debug("Marked {} notifications as read for user {}", unreadNotifications.size(), userId);
    }
    
    // Delete notification
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
    
    /**
     * NOTIFICATION: Patient Books Appointment → Doctor Gets Notification
     * 
     * Called automatically when a patient books an appointment.
     * Creates an APPOINTMENT_REQUESTED notification for the doctor.
     * This method runs asynchronously to avoid blocking the main request thread.
     * 
     * @param patient The patient who booked the appointment (actor)
     * @param doctor The doctor who will receive the notification (recipient)
     * @param appointmentId The ID of the newly created appointment
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createAppointmentRequestedNotification(Users patient, Users doctor, Long appointmentId) {
        // Check if the recipient (doctor) has appointment reminders enabled
        if (!notificationPreferencesService.isNotificationEnabled(doctor, "appointment_reminders")) {
            return;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(doctor);
        notification.setActor(patient);
        notification.setType(NotificationType.APPOINTMENT_REQUESTED);
        notification.setRelatedEntityId(appointmentId);
        notification.setMessage(patient.getFirstName() + " " + patient.getLastName() + " requested an appointment with you");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Evict unread count cache for the recipient since a new notification was created
        evictUnreadCountCache(doctor.getId());
    }
    
    /**
     * NOTIFICATION: Doctor Updates Appointment Status → Patient Gets Notification
     * 
     * Called automatically when a doctor confirms, cancels, or reschedules an appointment.
     * Creates the appropriate notification type for the patient:
     * - APPOINTMENT_CONFIRMED: Doctor confirmed the appointment
     * - APPOINTMENT_CANCELLED: Doctor cancelled the appointment
     * - APPOINTMENT_RESCHEDULED: Doctor rescheduled (includes new date/time in message)
     * This method runs asynchronously to avoid blocking the main request thread.
     * 
     * @param doctor The doctor who took the action (actor)
     * @param patient The patient who will receive the notification (recipient)
     * @param notificationType Type of notification (CONFIRMED, CANCELLED, or RESCHEDULED)
     * @param appointmentId The ID of the appointment being updated
     * @param additionalInfo Optional info (typically new date/time when rescheduling)
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createAppointmentStatusNotification(Users doctor, Users patient, NotificationType notificationType, Long appointmentId, String additionalInfo) {
        // Check if the recipient (patient) has appointment reminders enabled
        if (!notificationPreferencesService.isNotificationEnabled(patient, "appointment_reminders")) {
            return;
        }
        
        String message = "";
        switch (notificationType) {
            case APPOINTMENT_CONFIRMED:
                message = "Dr. " + doctor.getFirstName() + " " + doctor.getLastName() + " confirmed your appointment";
                break;
            case APPOINTMENT_CANCELLED:
                message = "Dr. " + doctor.getFirstName() + " " + doctor.getLastName() + " cancelled your appointment";
                break;
            case APPOINTMENT_RESCHEDULED:
                message = "Dr. " + doctor.getFirstName() + " " + doctor.getLastName() + " rescheduled your appointment";
                if (additionalInfo != null && !additionalInfo.isEmpty()) {
                    message += ": " + additionalInfo;
                }
                break;
            default:
                return; // Invalid notification type
        }
        
        Notification notification = new Notification();
        notification.setRecipient(patient);
        notification.setActor(doctor);
        notification.setType(notificationType);
        notification.setRelatedEntityId(appointmentId);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Evict unread count cache for the recipient since a new notification was created
        evictUnreadCountCache(patient.getId());
    }
    
    /**
     * NOTIFICATION: Patient Responds to Reschedule → Doctor Gets Notification
     * 
     * Called automatically when a patient responds to a reschedule request.
     * This completes the rescheduling workflow notification cycle:
     * 1. Doctor reschedules → Patient gets APPOINTMENT_RESCHEDULED notification
     * 2. Patient responds → Doctor gets this notification:
     *    - APPOINTMENT_RESCHEDULE_CONFIRMED: Patient accepted the new time
     *    - APPOINTMENT_RESCHEDULE_CANCELLED: Patient rejected the new time, appointment cancelled
     * This method runs asynchronously to avoid blocking the main request thread.
     * 
     * @param patient The patient who responded (actor)
     * @param doctor The doctor who will receive the notification (recipient)
     * @param notificationType Either APPOINTMENT_RESCHEDULE_CONFIRMED or APPOINTMENT_RESCHEDULE_CANCELLED
     * @param appointmentId The ID of the appointment
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createRescheduleResponseNotification(Users patient, Users doctor, NotificationType notificationType, Long appointmentId) {
        // Check if the recipient (doctor) has appointment reminders enabled
        if (!notificationPreferencesService.isNotificationEnabled(doctor, "appointment_reminders")) {
            return;
        }
        
        // Build message based on patient's response
        String message = "";
        switch (notificationType) {
            case APPOINTMENT_RESCHEDULE_CONFIRMED:
                message = patient.getFirstName() + " " + patient.getLastName() + " accepted the new appointment time";
                break;
            case APPOINTMENT_RESCHEDULE_CANCELLED:
                message = patient.getFirstName() + " " + patient.getLastName() + " rejected the rescheduled appointment time. The appointment has been cancelled";
                break;
            default:
                return; // Invalid notification type
        }
        
        Notification notification = new Notification();
        notification.setRecipient(doctor);
        notification.setActor(patient);
        notification.setType(notificationType);
        notification.setRelatedEntityId(appointmentId);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Evict unread count cache for the recipient since a new notification was created
        evictUnreadCountCache(doctor.getId());
    }
    
    /**
     * NOTIFICATION: Chat Message Sent → Recipient Gets Notification
     * 
     * Called automatically when a user sends a chat message.
     * Creates a CHAT_MESSAGE notification for the recipient (the other person in the chat).
     * This method runs asynchronously to avoid blocking the main request thread.
     * 
     * Example:
     * - Patient sends message → Doctor gets notification: "John Smith sent you a message"
     * - Doctor sends message → Patient gets notification: "Dr. Jane Doe sent you a message"
     * 
     * @param sender The user who sent the message (actor)
     * @param recipient The user who will receive the notification (the other person in the chat)
     * @param channelId The ID of the chat channel
     * @param messagePreview Optional preview of the message (first few words)
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createChatMessageNotification(Users sender, Users recipient, Long channelId, String messagePreview) {
        // Don't notify if sender is sending to themselves (shouldn't happen, but safety check)
        if (sender.getId().equals(recipient.getId())) {
            return;
        }
        
        // Check if the recipient has chat notifications enabled
        // Default to true if preference doesn't exist (chat_messages not yet in preferences)
        try {
            if (!notificationPreferencesService.isNotificationEnabled(recipient, "chat_messages")) {
                return;
            }
        } catch (Exception e) {
            // If preference check fails, default to enabled (allow notification)
            log.debug("Chat preference check failed, defaulting to enabled: {}", e.getMessage());
        }
        
        // Build notification message
        String senderName = sender.getFirstName() + " " + sender.getLastName();
        String message = senderName + " sent you a message";
        
        // Add message preview if provided (first 50 characters)
        if (messagePreview != null && !messagePreview.trim().isEmpty()) {
            String preview = messagePreview.length() > 50 
                ? messagePreview.substring(0, 50) + "..." 
                : messagePreview;
            message += ": \"" + preview + "\"";
        }
        
        // Create and save notification
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(sender);
        notification.setType(NotificationType.CHAT_MESSAGE);
        notification.setRelatedEntityId(channelId); // Store channel ID for navigation
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Evict unread count cache for the recipient since a new notification was created
        evictUnreadCountCache(recipient.getId());
    }
    
    /**
     * NOTIFICATION: Appointment Reminder → Both Patient and Doctor Get Notification
     * 
     * Called automatically by the scheduled reminder service.
     * Creates a 24-hour reminder notification for both patient and doctor.
     * This method runs asynchronously to avoid blocking the scheduled task.
     * 
     * @param patient The patient who has the appointment
     * @param doctor The doctor who has the appointment
     * @param notificationType Should be APPOINTMENT_REMINDER_24H
     * @param appointmentId The ID of the appointment
     * @param appointmentDateTime The date/time of the appointment (for message formatting)
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createAppointmentReminderNotification(Users patient, Users doctor, NotificationType notificationType, Long appointmentId, java.util.Date appointmentDateTime) {
        // Only support 24-hour reminders
        if (notificationType != NotificationType.APPOINTMENT_REMINDER_24H) {
            return;
        }
        
        // Format appointment date/time for the message
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
        String formattedDateTime = dateFormat.format(appointmentDateTime);
        
        // Create notification for PATIENT
        // Check if patient has appointment reminders enabled
        if (notificationPreferencesService.isNotificationEnabled(patient, "appointment_reminders")) {
            String patientMessage = "Reminder: You have an appointment with Dr. " + doctor.getFirstName() + " " + doctor.getLastName() + 
                     " in 24 hours (" + formattedDateTime + ")";
            
            Notification patientNotification = new Notification();
            patientNotification.setRecipient(patient);
            patientNotification.setActor(doctor); // Doctor is the "actor" for reminder context
            patientNotification.setType(NotificationType.APPOINTMENT_REMINDER_24H);
            patientNotification.setRelatedEntityId(appointmentId);
            patientNotification.setMessage(patientMessage);
            patientNotification.setIsRead(false);
            patientNotification.setCreatedAt(LocalDateTime.now());
            
            notificationRepository.save(patientNotification);
            
            // Evict unread count cache for the patient since a new notification was created
            evictUnreadCountCache(patient.getId());
        }
        
        // Create notification for DOCTOR
        // Check if doctor has appointment reminders enabled
        if (notificationPreferencesService.isNotificationEnabled(doctor, "appointment_reminders")) {
            String doctorMessage = "Reminder: You have an appointment with " + patient.getFirstName() + " " + patient.getLastName() + 
                     " in 24 hours (" + formattedDateTime + ")";
            
            Notification doctorNotification = new Notification();
            doctorNotification.setRecipient(doctor);
            doctorNotification.setActor(patient); // Patient is the "actor" for reminder context
            doctorNotification.setType(NotificationType.APPOINTMENT_REMINDER_24H);
            doctorNotification.setRelatedEntityId(appointmentId);
            doctorNotification.setMessage(doctorMessage);
            doctorNotification.setIsRead(false);
            doctorNotification.setCreatedAt(LocalDateTime.now());
            
            notificationRepository.save(doctorNotification);
            
            // Evict unread count cache for the doctor since a new notification was created
            evictUnreadCountCache(doctor.getId());
        }
    }
    
    // Helper method to convert notification to map
    private Map<String, Object> convertToMap(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId());
        map.put("type", notification.getType().name());
        map.put("message", notification.getMessage());
        map.put("isRead", notification.getIsRead());
        map.put("createdAt", notification.getCreatedAt().toString());
        map.put("relatedEntityId", notification.getRelatedEntityId());
        
        // Actor info
        Map<String, Object> actorMap = new HashMap<>();
        actorMap.put("id", notification.getActor().getId());
        actorMap.put("firstName", notification.getActor().getFirstName());
        actorMap.put("lastName", notification.getActor().getLastName());
        actorMap.put("profilePicture", notification.getActor().getProfilePicture());
        map.put("actor", actorMap);
        
        // Post info if available
        if (notification.getPost() != null) {
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("id", notification.getPost().getId());
            postMap.put("content", notification.getPost().getContent());
            map.put("post", postMap);
        }
        
        return map;
    }
}

