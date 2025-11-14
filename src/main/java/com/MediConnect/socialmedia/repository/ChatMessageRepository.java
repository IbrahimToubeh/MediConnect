package com.MediConnect.socialmedia.repository;

import com.MediConnect.socialmedia.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ChatMessage entities
 * 
 * Provides database operations for chat messages.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Get all messages in a channel, ordered by sent time (oldest first)
     * This is used to display the full conversation history
     */
    List<ChatMessage> findByChannelIdOrderBySentAtAsc(Long channelId);
    
    /**
     * Get the last message in a channel
     * Used to show message preview in channel list
     */
    ChatMessage findFirstByChannelIdOrderBySentAtDesc(Long channelId);
    
    /**
     * Count unread messages in a channel for a specific user
     * Messages are unread if:
     * 1. The message was sent by someone else (not the current user)
     * 2. The message has not been read yet (isRead = false)
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.channel.id = :channelId " +
           "AND m.sender.id != :userId AND m.isRead = false")
    Long countUnreadMessages(@Param("channelId") Long channelId, @Param("userId") Long userId);
    
    /**
     * Mark all messages in a channel as read for a specific user
     * This is called when a user opens a chat channel
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP " +
           "WHERE m.channel.id = :channelId AND m.sender.id != :userId AND m.isRead = false")
    void markMessagesAsRead(@Param("channelId") Long channelId, @Param("userId") Long userId);
}

