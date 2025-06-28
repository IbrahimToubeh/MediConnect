package com.MediConnect.EntryRelated.service.healthprovider.impl;

import com.MediConnect.EntryRelated.dto.healthprovider.GetAllSpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.service.healthprovider.HealthcareProviderService;
import com.MediConnect.EntryRelated.service.healthprovider.mapper.HealthcareProviderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthProviderServiceImpl implements HealthcareProviderService {

    private final HealthcareProviderRepo providerRepo;
    private final HealthcareProviderMapper providerMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public String register(SignupHPRequestDTO dto) {
        HealthcareProvider provider = providerMapper.signupDtoToProvider(dto);
        provider.setRole("HealthProvider");
        provider.setPassword(passwordEncoder.encode(provider.getPassword()));

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

            provider.setSpecializations(dto.getSpecializations());
        }

        providerRepo.save(provider);
        return "Success";
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
}
