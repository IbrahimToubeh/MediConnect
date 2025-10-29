package com.MediConnect.EntryRelated.repository;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthcareProviderRepo extends JpaRepository<HealthcareProvider, Long> {
    Optional<HealthcareProvider> findByUsername(String username);

    Optional<HealthcareProvider> findByEmail(String email);

    Optional<HealthcareProvider> findByLicenseNumber(String licenseNumber);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);
}