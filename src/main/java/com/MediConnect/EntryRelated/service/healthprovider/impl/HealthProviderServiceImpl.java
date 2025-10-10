package com.MediConnect.EntryRelated.service.healthprovider.impl;

import com.MediConnect.EntryRelated.dto.healthprovider.GetAllSpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.SpecializationType;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.service.healthprovider.HealthcareProviderService;
import com.MediConnect.EntryRelated.service.healthprovider.mapper.HealthcareProviderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HealthProviderServiceImpl implements HealthcareProviderService {

    private final HealthcareProviderRepo providerRepo;
    private final HealthcareProviderMapper providerMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public String register(SignupHPRequestDTO dto) {
        // Normalize email
        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        
        // Check for duplicate username
        if (providerRepo.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check for duplicate email
        if (providerRepo.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already exists");
        }
        
        HealthcareProvider provider = providerMapper.signupDtoToProvider(dto);
        provider.setRole("HEALTHPROVIDER");
        provider.setEmail(normalizedEmail);
        provider.setRegistrationDate(new Date());
        // Get password from DTO since mapper ignores it
        provider.setPassword(passwordEncoder.encode(dto.getPassword()));
        
        // Convert date of birth from string to Date
        if (dto.getDateOfBirth() != null && !dto.getDateOfBirth().isEmpty()) {
            try {
                String dateString = dto.getDateOfBirth().trim();
                System.out.println("DEBUG: Attempting to parse dateOfBirth: '" + dateString + "'");
                
                // If ISO format (contains T), extract just the date part
                if (dateString.contains("T")) {
                    dateString = dateString.substring(0, dateString.indexOf("T")).trim();
                    System.out.println("DEBUG: Extracted date part: '" + dateString + "'");
                }
                
                // Parse the date in simple yyyy-MM-dd format
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                Date parsed = sdf.parse(dateString);
                provider.setDateOfBirth(parsed);
                System.out.println("DEBUG: Successfully parsed date: " + parsed);
            } catch (Exception e) {
                System.out.println("ERROR: Failed to parse date of birth: '" + dto.getDateOfBirth() + "'");
                System.out.println("ERROR: Exception message: " + e.getMessage());
                e.printStackTrace();
                // Don't throw, just skip setting the date
            }
        }

        if (dto.getEducationHistories() != null) {
            List<EducationHistory> educationHistories = providerMapper.mapEducationHistories(dto.getEducationHistories());
            educationHistories.forEach(e -> e.setProvider(provider));
            provider.setEducationHistories(educationHistories);
        }

        if (dto.getWorkExperiences() != null) {
            List<WorkExperience> workExperiences = providerMapper.mapWorkExperiences(dto.getWorkExperiences());
            workExperiences.forEach(w -> w.setProvider(provider));
            provider.setWorkExperiences(workExperiences);
        }

        if (dto.getSpecializations() != null) {
            // Convert string list to SpecializationType enum list
            List<SpecializationType> specializationTypes = dto.getSpecializations().stream()
                    .map(spec -> {
                        try {
                            return SpecializationType.valueOf(spec);
                        } catch (IllegalArgumentException e) {
                            System.out.println("Invalid specialization: " + spec);
                            return null;
                        }
                    })
                    .filter(spec -> spec != null)
                    .collect(java.util.stream.Collectors.toList());
            provider.setSpecializations(specializationTypes);
        }

        providerRepo.save(provider);
        return "Healthcare provider registered successfully";
    }

    @Override
    public List<GetAllSpecialtyDTO> getAllSpecialtyDTO() {
        return List.of();
    }


    public HealthcareProvider getProviderProfile(Long id) {
        return providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
    }

    public List<HealthcareProvider> getAllProviders() {
        return providerRepo.findAll();
    }

    public HealthcareProvider updateProviderProfile(HealthcareProvider updatedProvider) {
        return providerRepo.save(updatedProvider);
    }

    @Override
    public Optional<HealthcareProvider> findByUsername(String username) {
        return providerRepo.findByUsername(username);
    }

    @Override
    public HealthcareProvider save(HealthcareProvider provider) {
        return providerRepo.save(provider);
    }
}
