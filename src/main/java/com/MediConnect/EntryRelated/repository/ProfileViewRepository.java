package com.MediConnect.EntryRelated.repository;

import com.MediConnect.EntryRelated.entities.ProfileView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * Repository for ProfileView entity.
 * 
 * Provides methods to query profile view statistics for analytics.
 */
@Repository
public interface ProfileViewRepository extends JpaRepository<ProfileView, Long> {
    
    /**
     * Count total profile views for a specific doctor
     * 
     * @param doctorId The ID of the healthcare provider
     * @return Total number of profile views
     */
    long countByDoctorId(Long doctorId);
    
    /**
     * Count profile views for a doctor within a date range
     * 
     * @param doctorId The ID of the healthcare provider
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (inclusive)
     * @return Number of profile views in the date range
     */
    @Query("SELECT COUNT(pv) FROM ProfileView pv WHERE pv.doctorId = :doctorId " +
           "AND pv.viewedAt >= :startDate AND pv.viewedAt <= :endDate")
    long countByDoctorIdAndDateRange(@Param("doctorId") Long doctorId, 
                                      @Param("startDate") Date startDate, 
                                      @Param("endDate") Date endDate);
}

