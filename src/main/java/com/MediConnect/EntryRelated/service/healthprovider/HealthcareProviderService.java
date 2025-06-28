package com.MediConnect.EntryRelated.service.healthprovider;

import com.MediConnect.EntryRelated.dto.healthprovider.GetAllSpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface HealthcareProviderService {
     String register(SignupHPRequestDTO signupHPRequestDTO);
     List<GetAllSpecialtyDTO> getAllSpecialtyDTO();
}
