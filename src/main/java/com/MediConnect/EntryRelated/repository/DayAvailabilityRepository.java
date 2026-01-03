package com.MediConnect.EntryRelated.repository;

import com.MediConnect.EntryRelated.entities.DayAvailability;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DayAvailabilityRepository extends JpaRepository<DayAvailability, Long> {
    List<DayAvailability> findByProvider(HealthcareProvider provider);
    Optional<DayAvailability> findByProviderAndDayOfWeek(HealthcareProvider provider, String dayOfWeek);
    void deleteByProvider(HealthcareProvider provider);
}

