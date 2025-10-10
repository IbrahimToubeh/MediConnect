package com.MediConnect.EntryRelated.service.healthprovider;

import com.MediConnect.EntryRelated.dto.healthprovider.GetAllSpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface HealthcareProviderService {
    String register(SignupHPRequestDTO signupHPRequestDTO);

    List<GetAllSpecialtyDTO> getAllSpecialtyDTO();
    
    Optional<HealthcareProvider> findByUsername(String username);
    
    HealthcareProvider save(HealthcareProvider provider);
}
