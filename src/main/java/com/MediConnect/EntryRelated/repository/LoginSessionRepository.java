package com.MediConnect.EntryRelated.repository;

import com.MediConnect.EntryRelated.entities.LoginSession;
import com.MediConnect.EntryRelated.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {
    
    List<LoginSession> findByUserOrderByLoginTimeDesc(Users user);
    
    List<LoginSession> findByUserAndIsActiveTrueOrderByLoginTimeDesc(Users user);
    
    Optional<LoginSession> findBySessionToken(String sessionToken);
    
    @Query("SELECT ls FROM LoginSession ls WHERE ls.user = :user AND ls.isActive = true ORDER BY ls.loginTime DESC")
    List<LoginSession> findActiveSessionsByUser(@Param("user") Users user);
    
    @Query("SELECT ls FROM LoginSession ls WHERE ls.user = :user ORDER BY ls.loginTime DESC LIMIT :limit")
    List<LoginSession> findRecentSessionsByUser(@Param("user") Users user, @Param("limit") int limit);
    
    void deleteByUserAndIsActiveFalseAndLogoutTimeBefore(Users user, LocalDateTime cutoffTime);
    
    // Find sessions that haven't been active for a certain period
    @Query("SELECT ls FROM LoginSession ls WHERE ls.user = :user AND ls.isActive = true AND ls.lastActivityTime < :cutoffTime")
    List<LoginSession> findInactiveSessions(@Param("user") Users user, @Param("cutoffTime") LocalDateTime cutoffTime);
}
