package com.MediConnect.EntryRelated.service.healthprovider;

import com.MediConnect.EntryRelated.dto.healthprovider.GetAllSpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.LoginHPRequestDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public interface HealthcareProviderService {
    String register(SignupHPRequestDTO signupHPRequestDTO);

    Map<String, Object> loginProvider(LoginHPRequestDTO loginRequest, HttpServletRequest request);
    Map<String, Object> verifyLoginOTP(Map<String, String> request, HttpServletRequest httpRequest);

    List<GetAllSpecialtyDTO> getAllSpecialtyDTO();

    Optional<HealthcareProvider> findByUsername(String username);

    Optional<HealthcareProvider> findById(Long id);

    HealthcareProvider save(HealthcareProvider provider);

    List<HealthcareProvider> searchDoctors(String name, String city, String specialty, String insurance, Double minFee, Double maxFee, Double minRating);

    Map<String, Object> getProviderProfileByUsername(String username);

    Map<String, Object> updateProviderProfileByUsername(String username, HealthcareProvider updatedData);
}
