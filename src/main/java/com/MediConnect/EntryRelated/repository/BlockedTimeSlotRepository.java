package com.MediConnect.EntryRelated.repository;

import com.MediConnect.EntryRelated.entities.BlockedTimeSlot;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BlockedTimeSlotRepository extends JpaRepository<BlockedTimeSlot, Long> {
    List<BlockedTimeSlot> findByProvider(HealthcareProvider provider);
    
    @Query("SELECT b FROM BlockedTimeSlot b WHERE b.provider = :provider AND b.blockedDate = :date")
    List<BlockedTimeSlot> findByProviderAndDate(@Param("provider") HealthcareProvider provider, @Param("date") Date date);
    
    @Query("SELECT b FROM BlockedTimeSlot b WHERE b.provider = :provider AND b.blockedDate >= :startDate AND b.blockedDate <= :endDate")
    List<BlockedTimeSlot> findByProviderAndDateRange(@Param("provider") HealthcareProvider provider, 
                                                      @Param("startDate") Date startDate, 
                                                      @Param("endDate") Date endDate);
    
    void deleteByProvider(HealthcareProvider provider);
}

