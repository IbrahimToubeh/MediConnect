package com.MediConnect.socialmedia.service;

import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.service.NotificationPreferencesService;
import com.MediConnect.socialmedia.entity.*;
import com.MediConnect.socialmedia.repository.NotificationRepository;
import com.MediConnect.Repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private NotificationPreferencesService notificationPreferencesService;
    
    // Create notification when someone likes a post
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
    }
    
    // Create notification when someone comments on a post
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
    }
    
    // Create notification when someone likes a comment
    public void createCommentLikeNotification(Users actor, MedicalPostComment comment) {
        System.out.println("=== CREATE COMMENT LIKE NOTIFICATION ===");
        System.out.println("Actor: " + actor.getUsername() + " (ID: " + actor.getId() + ")");
        System.out.println("Comment ID: " + comment.getId() + ", Commenter ID: " + comment.getCommenterId());
        
        // Don't notify if the actor is liking their own comment
        Users commentOwner = userRepo.findById(comment.getCommenterId()).orElse(null);
        if (commentOwner == null) {
            System.out.println("Comment owner not found!");
            return;
        }
        
        System.out.println("Comment owner: " + commentOwner.getUsername() + " (ID: " + commentOwner.getId() + ")");
        
        if (commentOwner.getId().equals(actor.getId())) {
            System.out.println("Actor is commenting on own comment - no notification");
            return;
        }
        
        // Check if the recipient has comment likes notifications enabled (using post_likes for now)
        boolean notificationsEnabled = notificationPreferencesService.isNotificationEnabled(commentOwner, "post_likes");
        System.out.println("Notifications enabled for user: " + notificationsEnabled);
        
        if (!notificationsEnabled) {
            System.out.println("Notifications disabled for this user - no notification created");
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
        System.out.println("Comment like notification created successfully!");
        System.out.println("=== END CREATE COMMENT LIKE NOTIFICATION ===");
    }
    
    // Create notification when someone replies to a comment
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
    }
    
    // Get all notifications for a user
    public List<Map<String, Object>> getUserNotifications(Long userId) {
        Users user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
        
        return notifications.stream()
            .map(this::convertToMap)
            .collect(Collectors.toList());
    }
    
    // Get unread notification count
    public Long getUnreadCount(Long userId) {
        Users user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }
    
    // Mark notification as read
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
    
    // Mark all notifications as read for a user
    @Transactional
    public void markAllAsRead(Long userId) {
        Users user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(user);
        
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }
    
    // Delete notification
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
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

