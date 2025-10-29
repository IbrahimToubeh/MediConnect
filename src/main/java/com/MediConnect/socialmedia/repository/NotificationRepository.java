package com.MediConnect.socialmedia.repository;

import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.socialmedia.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByRecipientOrderByCreatedAtDesc(Users recipient);
    
    List<Notification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(Users recipient);
    
    Long countByRecipientAndIsReadFalse(Users recipient);
    
    void deleteByRecipient(Users recipient);
}


