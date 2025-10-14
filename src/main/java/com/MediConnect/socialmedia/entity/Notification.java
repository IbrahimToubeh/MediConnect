package com.MediConnect.socialmedia.entity;

import com.MediConnect.EntryRelated.entities.Users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private Users recipient;
    
    @ManyToOne
    @JoinColumn(name = "actor_id", nullable = false)
    private Users actor; // Person who triggered the notification
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @ManyToOne
    @JoinColumn(name = "post_id")
    private MedicalPost post;
    
    @ManyToOne
    @JoinColumn(name = "comment_id")
    private MedicalPostComment comment;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(nullable = false)
    private Boolean isRead = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Link to the specific item (post ID, comment ID, etc.)
    private Long relatedEntityId;
}


