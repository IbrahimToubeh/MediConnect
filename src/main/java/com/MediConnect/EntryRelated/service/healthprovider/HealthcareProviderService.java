package com.MediConnect.EntryRelated.service.healthprovider;

import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import org.springframework.stereotype.Component;

@Component
public interface HealthcareProviderService {
     String register(SignupHPRequestDTO signupHPRequestDTO);
}
