package com.MediConnect.EntryRelated.repository;

import com.MediConnect.EntryRelated.entities.AccountActivity;
import com.MediConnect.EntryRelated.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountActivityRepository extends JpaRepository<AccountActivity, Long> {
    
    List<AccountActivity> findByUserOrderByTimestampDesc(Users user);
    
    @Query("SELECT aa FROM AccountActivity aa WHERE aa.user = :user ORDER BY aa.timestamp DESC LIMIT :limit")
    List<AccountActivity> findRecentActivitiesByUser(@Param("user") Users user, @Param("limit") int limit);
    
    List<AccountActivity> findByUserAndTypeOrderByTimestampDesc(Users user, AccountActivity.ActivityType type);
    
    // Find activities within a date range
    @Query("SELECT aa FROM AccountActivity aa WHERE aa.user = :user AND aa.timestamp BETWEEN :startDate AND :endDate ORDER BY aa.timestamp DESC")
    List<AccountActivity> findActivitiesByUserAndDateRange(
        @Param("user") Users user, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    // Delete old activities (cleanup)
    void deleteByUserAndTimestampBefore(Users user, LocalDateTime cutoffTime);
}
