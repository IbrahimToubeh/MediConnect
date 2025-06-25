package com.MediConnect.EntryRelated.service.healthprovider.impl;

import com.MediConnect.EntryRelated.dto.healthprovider.LoginHPRequestDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.service.healthprovider.HealthProviderService;
import com.MediConnect.EntryRelated.service.healthprovider.mapper.HealthcareProviderMapper;
import com.MediConnect.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HealthProviderServiceImpl implements HealthProviderService {

    private final HealthcareProviderRepo healthcareProviderRepo;
    private final UserService userService;
    private final HealthcareProviderMapper healthcareProviderMapper;



    @Override
    @Transactional
    public String register(SignupHPRequestDTO signupHPRequestDTO) {
        try {
            // Validation checks
            if (healthcareProviderRepo.existsByUsername(signupHPRequestDTO.getUsername())) {
                return "Username already exists";
            }
            if (healthcareProviderRepo.existsByEmail(signupHPRequestDTO.getEmail())) {
                return "Email already exists";
            }
            if (healthcareProviderRepo.existsByLicenseNumber(signupHPRequestDTO.getLicenseNumber())) {
                return "License number already exists";
            }

            HealthcareProvider provider = healthcareProviderMapper.signupDtoToProvider(signupHPRequestDTO);

            userService.registerUser(provider);

            return "Healthcare Provider registered successfully";
        } catch (Exception e) {
            return "Registration failed: " + e.getMessage();
        }
    }
}