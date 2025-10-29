package com.MediConnect.socialmedia.entity;

public enum NotificationType {
    POST_LIKE,           // Someone liked your post
    POST_COMMENT,        // Someone commented on your post
    COMMENT_LIKE,        // Someone liked your comment
    COMMENT_REPLY,       // Someone replied to your comment
    POST_SHARE,          // Someone shared your post (future)
    
    // Appointment-related notifications
    APPOINTMENT_REQUESTED,           // Appointment request sent
    APPOINTMENT_CONFIRMED,           // Appointment confirmed by doctor
    APPOINTMENT_CANCELLED,           // Appointment cancelled
    APPOINTMENT_RESCHEDULED,         // Appointment rescheduled
    APPOINTMENT_RESCHEDULE_CONFIRMED, // Reschedule request confirmed
    APPOINTMENT_RESCHEDULE_CANCELLED  // Reschedule request cancelled
}


