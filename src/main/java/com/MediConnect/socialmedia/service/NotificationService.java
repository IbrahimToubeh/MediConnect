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

    @Cacheable(value = "adminUsers", key = "'all'")
    @Transactional(readOnly = true)
    public List<Users> getAdminUsers() {
        log.debug("Fetching admin users from database (cache miss)");
        return userRepo.findByRoleIgnoreCase("ADMIN");
    }

    @Async("notificationTaskExecutor")
    @Transactional
    public void createAdminNotification(Users actor, String message, NotificationType notificationType, Long relatedEntityId) {
        if (actor == null || notificationType == null) return;

        List<Users> admins = getAdminUsers();
        if (admins == null || admins.isEmpty()) return;

        for (Users admin : admins) {
            if (admin.getId().equals(actor.getId())) continue;

            Notification notification = new Notification();
            notification.setRecipient(admin);
            notification.setActor(actor);
            notification.setType(notificationType);
            notification.setRelatedEntityId(relatedEntityId);
            notification.setMessage(message);
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);

            evictUnreadCountCache(admin.getId());
        }
    }

    // --- SOCIAL MEDIA NOTIFICATIONS (Keep accepting Entities for now) ---

    @Async("notificationTaskExecutor")
    @Transactional
    public void createPostLikeNotification(Users actor, MedicalPost post) {
        if (post.getPostProvider().getId().equals(actor.getId())) return;
        if (!notificationPreferencesService.isNotificationEnabled(post.getPostProvider(), "post_likes")) return;

        saveNotification(post.getPostProvider(), actor, NotificationType.POST_LIKE,
                actor.getFirstName() + " " + actor.getLastName() + " liked your post", post.getId(), post, null);
    }

    @Async("notificationTaskExecutor")
    @Transactional
    public void createPostCommentNotification(Users actor, MedicalPost post, MedicalPostComment comment) {
        if (post.getPostProvider().getId().equals(actor.getId())) return;
        if (!notificationPreferencesService.isNotificationEnabled(post.getPostProvider(), "post_comments")) return;

        saveNotification(post.getPostProvider(), actor, NotificationType.POST_COMMENT,
                actor.getFirstName() + " " + actor.getLastName() + " commented on your post", post.getId(), post, comment);
    }

    @Async("notificationTaskExecutor")
    @Transactional
    public void createCommentLikeNotification(Users actor, MedicalPostComment comment) {
        Users commentOwner = userRepo.findById(comment.getCommenterId()).orElse(null);
        if (commentOwner == null || commentOwner.getId().equals(actor.getId())) return;

        if (!notificationPreferencesService.isNotificationEnabled(commentOwner, "comment_likes")) return;

        saveNotification(commentOwner, actor, NotificationType.COMMENT_LIKE,
                actor.getFirstName() + " " + actor.getLastName() + " liked your comment",
                comment.getPost().getId(), comment.getPost(), comment);
    }

    @Async("notificationTaskExecutor")
    @Transactional
    public void createCommentReplyNotification(Users actor, CommentReply reply, MedicalPostComment parentComment) {
        Users commentOwner = userRepo.findById(parentComment.getCommenterId()).orElse(null);
        if (commentOwner == null || commentOwner.getId().equals(actor.getId())) return;

        if (!notificationPreferencesService.isNotificationEnabled(commentOwner, "comment_replies")) return;

        saveNotification(commentOwner, actor, NotificationType.COMMENT_REPLY,
                actor.getFirstName() + " " + actor.getLastName() + " replied to your comment",
                parentComment.getPost().getId(), parentComment.getPost(), parentComment);
    }

    // --- APPOINTMENT NOTIFICATIONS (UPDATED TO USE IDs) ---

    /**
     * 1. Appointment Requested (Updated to accept IDs)
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createAppointmentRequestedNotification(Long patientId, Long doctorId, Long appointmentId) {
        // Fetch entities fresh inside the Async thread
        Users patient = userRepo.findById(patientId).orElse(null);
        Users doctor = userRepo.findById(doctorId).orElse(null);

        if (patient == null || doctor == null) return;

        if (!notificationPreferencesService.isNotificationEnabled(doctor, "appointment_reminders")) {
            return;
        }

        String message = patient.getFirstName() + " " + patient.getLastName() + " requested an appointment with you";
        saveNotification(doctor, patient, NotificationType.APPOINTMENT_REQUESTED, message, appointmentId, null, null);
    }

    /**
     * 2. Appointment Status Update (Updated to accept IDs)
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createAppointmentStatusNotification(Long doctorId, Long patientId, NotificationType notificationType, Long appointmentId, String additionalInfo) {
        // Fetch entities fresh
        Users doctor = userRepo.findById(doctorId).orElse(null);
        Users patient = userRepo.findById(patientId).orElse(null);

        if (doctor == null || patient == null) return;

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
            default: return;
        }

        saveNotification(patient, doctor, notificationType, message, appointmentId, null, null);
    }

    /**
     * 3. Reschedule Response (Updated to accept IDs)
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void createRescheduleResponseNotification(Long patientId, Long doctorId, NotificationType notificationType, Long appointmentId) {
        // Fetch entities fresh
        Users patient = userRepo.findById(patientId).orElse(null);
        Users doctor = userRepo.findById(doctorId).orElse(null);

        if (patient == null || doctor == null) return;

        if (!notificationPreferencesService.isNotificationEnabled(doctor, "appointment_reminders")) {
            return;
        }

        String message = "";
        switch (notificationType) {
            case APPOINTMENT_RESCHEDULE_CONFIRMED:
                message = patient.getFirstName() + " " + patient.getLastName() + " accepted the new appointment time";
                break;
            case APPOINTMENT_RESCHEDULE_CANCELLED:
                message = patient.getFirstName() + " " + patient.getLastName() + " rejected the rescheduled appointment time. The appointment has been cancelled";
                break;
            default: return;
        }

        saveNotification(doctor, patient, notificationType, message, appointmentId, null, null);
    }

    // --- OTHER NOTIFICATIONS ---

    @Async("notificationTaskExecutor")
    @Transactional
    public void createChatMessageNotification(Users sender, Users recipient, Long channelId, String messagePreview) {
        if (sender.getId().equals(recipient.getId())) return;

        try {
            if (!notificationPreferencesService.isNotificationEnabled(recipient, "chat_messages")) return;
        } catch (Exception e) {
            // Ignore preference error
        }

        String senderName = sender.getFirstName() + " " + sender.getLastName();
        String message = senderName + " sent you a message";
        if (messagePreview != null && !messagePreview.trim().isEmpty()) {
            String preview = messagePreview.length() > 50 ? messagePreview.substring(0, 50) + "..." : messagePreview;
            message += ": \"" + preview + "\"";
        }

        saveNotification(recipient, sender, NotificationType.CHAT_MESSAGE, message, channelId, null, null);
    }

    @Async("notificationTaskExecutor")
    @Transactional
    public void createAppointmentReminderNotification(Users patient, Users doctor, NotificationType notificationType, Long appointmentId, java.util.Date appointmentDateTime) {
        if (notificationType != NotificationType.APPOINTMENT_REMINDER_24H) return;

        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
        String formattedDateTime = dateFormat.format(appointmentDateTime);

        try {
            if (notificationPreferencesService.isNotificationEnabled(patient, "appointment_reminders")) {
                String msg = "Reminder: You have an appointment with Dr. " + doctor.getFirstName() + " " + doctor.getLastName() + " in 24 hours (" + formattedDateTime + ")";
                saveNotification(patient, doctor, NotificationType.APPOINTMENT_REMINDER_24H, msg, appointmentId, null, null);
            }
        } catch (Exception e) { log.warn("Failed to send patient reminder", e); }

        try {
            if (notificationPreferencesService.isNotificationEnabled(doctor, "appointment_reminders")) {
                String msg = "Reminder: You have an appointment with " + patient.getFirstName() + " " + patient.getLastName() + " in 24 hours (" + formattedDateTime + ")";
                saveNotification(doctor, patient, NotificationType.APPOINTMENT_REMINDER_24H, msg, appointmentId, null, null);
            }
        } catch (Exception e) { log.warn("Failed to send doctor reminder", e); }
    }

    // --- HELPER METHODS ---

    private void saveNotification(Users recipient, Users actor, NotificationType type, String message, Long relatedId, MedicalPost post, MedicalPostComment comment) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(type);
        notification.setRelatedEntityId(relatedId);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        if (post != null) notification.setPost(post);
        if (comment != null) notification.setComment(comment);

        notificationRepository.save(notification);
        evictUnreadCountCache(recipient.getId());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserNotifications(Long userId) {
        Users user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user).stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "unreadCount", key = "#userId")
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        Users user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Long count = notificationRepository.countByRecipientAndIsReadFalse(user);
        return count != null ? Long.valueOf(count) : 0L;
    }

    @CacheEvict(value = "unreadCount", key = "#result?.recipient?.id", beforeInvocation = false)
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        evictUnreadCountCache(notification.getRecipient().getId());
    }

    @CacheEvict(value = "unreadCount", key = "#userId")
    public void evictUnreadCountCache(Long userId) {
        log.debug("Evicting unread count cache for user {}", userId);
    }

    @CacheEvict(value = "unreadCount", key = "#userId")
    @Transactional
    public void markAllAsRead(Long userId) {
        Users user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(user);
        unreadNotifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    private Map<String, Object> convertToMap(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId());
        map.put("type", notification.getType().name());
        map.put("message", notification.getMessage());
        map.put("isRead", notification.getIsRead());
        map.put("createdAt", notification.getCreatedAt().toString());
        map.put("relatedEntityId", notification.getRelatedEntityId());

        Map<String, Object> actorMap = new HashMap<>();
        actorMap.put("id", notification.getActor().getId());
        actorMap.put("firstName", notification.getActor().getFirstName());
        actorMap.put("lastName", notification.getActor().getLastName());
        actorMap.put("profilePicture", notification.getActor().getProfilePicture());
        map.put("actor", actorMap);

        if (notification.getPost() != null) {
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("id", notification.getPost().getId());
            postMap.put("content", notification.getPost().getContent());
            map.put("post", postMap);
        }
        return map;
    }
}