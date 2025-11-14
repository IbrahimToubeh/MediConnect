package com.MediConnect.socialmedia.entity;

import com.MediConnect.EntryRelated.entities.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Chat Message Entity
 * 
 * Represents a single message within a chat channel.
 * 
 * Business Rules:
 * - Each message belongs to one chat channel
 * - Each message has a sender (can be patient or doctor)
 * - Messages track read status for read receipts
 * - Messages are ordered by sentAt timestamp
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The chat channel this message belongs to
     * Required - every message must belong to a channel
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private ChatChannel channel;
    
    /**
     * The user who sent this message
     * Can be either a Patient or HealthcareProvider
     * Uses Users as the base type since both inherit from it
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Users sender;
    
    /**
     * The message content
     * Text content of the message
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    /**
     * When this message was sent
     * Automatically set when message is created
     */
    @Column(name = "sent_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentAt;
    
    /**
     * Whether this message has been read by the recipient
     * Used for read receipts and unread message counts
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    /**
     * When this message was read
     * Null if message hasn't been read yet
     */
    @Column(name = "read_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date readAt;
    
    /**
     * Whether this message has been deleted (soft delete)
     * Used for soft deletion instead of permanently deleting messages
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    /**
     * Set default timestamp before saving
     */
    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = new Date();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}

