package com.MediConnect.socialmedia.entity;

public enum NotificationType {
    POST_LIKE,           // Someone liked your post
    POST_COMMENT,        // Someone commented on your post
    COMMENT_LIKE,        // Someone liked your comment
    COMMENT_REPLY,       // Someone replied to your comment
    POST_SHARE,          // Someone shared your post (future)
    
    // ===== APPOINTMENT-RELATED NOTIFICATIONS =====
    // These notification types are used for the appointment booking and management workflow
    
    APPOINTMENT_REQUESTED,           // Sent to doctor when patient books an appointment
    APPOINTMENT_CONFIRMED,           // Sent to patient when doctor confirms appointment
    APPOINTMENT_CANCELLED,           // Sent to patient when doctor cancels appointment
    APPOINTMENT_RESCHEDULED,         // Sent to patient when doctor reschedules appointment (includes new time)
    APPOINTMENT_RESCHEDULE_CONFIRMED, // Sent to doctor when patient accepts rescheduled time
    APPOINTMENT_RESCHEDULE_CANCELLED  // Sent to doctor when patient rejects rescheduled time (appointment cancelled)
}


