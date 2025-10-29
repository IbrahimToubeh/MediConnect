package com.MediConnect.EntryRelated.service.healthprovider.impl;

import com.MediConnect.EntryRelated.dto.healthprovider.GetAllSpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.LoginHPRequestDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.SpecializationType;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.service.ActivityService;
import com.MediConnect.EntryRelated.service.OTPService;
import com.MediConnect.EntryRelated.service.healthprovider.HealthcareProviderService;
import com.MediConnect.EntryRelated.service.healthprovider.mapper.HealthcareProviderMapper;
import com.MediConnect.Service.UserService;
import com.MediConnect.config.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HealthProviderServiceImpl implements HealthcareProviderService {

    private final HealthcareProviderRepo providerRepo;
    private final HealthcareProviderMapper providerMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserService userService;
    private final OTPService otpService;
    private final JWTService jwtService;
    private final ActivityService activityService;
    @Transactional
    public String register(SignupHPRequestDTO dto) {
        try {
            System.out.println("=== DOCTOR REGISTRATION START ===");
            System.out.println("Username: " + dto.getUsername());
            System.out.println("Email: " + dto.getEmail());
            System.out.println("Insurance Accepted: " + (dto.getInsuranceAccepted() != null ? dto.getInsuranceAccepted() : "null"));
            
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
            
            System.out.println("Mapping DTO to Provider entity...");
            HealthcareProvider provider = providerMapper.signupDtoToProvider(dto);
            System.out.println("Mapping successful");
            
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

        System.out.println("Saving provider to database...");
        providerRepo.save(provider);
        System.out.println("Provider saved successfully with ID: " + provider.getId());
        System.out.println("=== DOCTOR REGISTRATION END ===");
        return "Healthcare provider registered successfully";
        } catch (Exception e) {
            System.err.println("ERROR during doctor registration: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Map<String, Object> loginProvider(LoginHPRequestDTO loginRequest, HttpServletRequest request) {
        System.out.println("DEBUG LOGIN: Attempting login for username: '" + loginRequest.getUsername() + "'");

        // 1. Verify the provider exists
        HealthcareProvider provider = providerRepo.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

        System.out.println("DEBUG LOGIN: Found provider with role: '" + provider.getRole() + "'");

        // 2. Check role
        if (!"HEALTHPROVIDER".equals(provider.getRole())) {
            throw new RuntimeException("Access denied: This account is not a healthcare provider account");
        }

        // 3. Authenticate credentials
        userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

        // 4. Handle 2FA (OTP)
        if (userService.isTwoFactorEnabled(loginRequest.getUsername())) {
            otpService.sendLoginOTP(provider.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "2fa_required");
            response.put("message", "OTP sent to your email. Please verify to complete login.");
            response.put("email", provider.getEmail());
            response.put("username", loginRequest.getUsername());
            response.put("userId", provider.getId());
            return response;
        }

        // 5. Generate token
        String token = jwtService.generateToken(new com.MediConnect.config.UserPrincipal(provider));

        // 6. Log session activity
        activityService.createLoginSession(provider, token, request);

        System.out.println("DEBUG LOGIN: Authentication successful, token generated");

        // 7. Return success response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Healthcare provider login successful");
        response.put("token", token);
        response.put("userId", provider.getId());
        return response;
    }

    @Override
    public Map<String, Object> verifyLoginOTP(Map<String, String> request, HttpServletRequest httpRequest) {
        String username = request.get("username");
        String otp = request.get("otp");

        HealthcareProvider provider = providerRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

        // Verify OTP
        if (!otpService.verifyLoginOTP(provider.getEmail(), otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Clear the OTP
        otpService.clearLoginOTP(provider.getEmail());

        // Generate token
        String token = jwtService.generateToken(new com.MediConnect.config.UserPrincipal(provider));

        // Create login session and log activity
        activityService.createLoginSession(provider, token, httpRequest);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Login successful");
        response.put("token", token);
        response.put("userId", provider.getId());
        return response;
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
    public Optional<HealthcareProvider> findById(Long id) {
        return providerRepo.findById(id);
    }

    @Override
    public HealthcareProvider save(HealthcareProvider provider) {
        return providerRepo.save(provider);
    }

    @Override
    public List<HealthcareProvider> searchDoctors(String name, String city, String specialty, String insurance, Double minFee, Double maxFee, Double minRating) {
        List<HealthcareProvider> allProviders = providerRepo.findAll();
        System.out.println("DEBUG: Total providers in database: " + allProviders.size());
        
        return allProviders.stream()
            .filter(provider -> {
                // Filter by name (firstName or lastName) - more flexible matching
                if (name != null && !name.trim().isEmpty()) {
                    // Remove "Dr." or "Dr" prefix if present (case-insensitive)
                    String cleanedSearchName = name.trim().replaceAll("(?i)^dr\\.?\\s*", "");
                    String lowerName = cleanedSearchName.toLowerCase().trim();
                    
                    String firstName = provider.getFirstName() != null ? provider.getFirstName() : "";
                    String lastName = provider.getLastName() != null ? provider.getLastName() : "";
                    String fullName = (firstName + " " + lastName).toLowerCase().trim();
                    
                    // Match against first name, last name, or full name
                    boolean nameMatch = firstName.toLowerCase().contains(lowerName) ||
                                       lastName.toLowerCase().contains(lowerName) ||
                                       fullName.contains(lowerName);
                    
                    System.out.println("DEBUG: Searching for '" + name + "' (cleaned: '" + cleanedSearchName + "') in doctor '" + provider.getFirstName() + " " + provider.getLastName() + "' - Match: " + nameMatch);
                    if (!nameMatch) return false;
                }
                
                // Filter by city
                if (city != null && !city.trim().isEmpty()) {
                    if (provider.getCity() == null || !provider.getCity().toLowerCase().contains(city.toLowerCase())) {
                        return false;
                    }
                }
                
                // Filter by specialty
                if (specialty != null && !specialty.trim().isEmpty()) {
                    if (provider.getSpecializations() == null || provider.getSpecializations().isEmpty()) {
                        return false;
                    }
                    boolean hasSpecialty = provider.getSpecializations().stream()
                        .anyMatch(spec -> spec.name().equalsIgnoreCase(specialty));
                    if (!hasSpecialty) return false;
                }
                
                // Filter by insurance
                if (insurance != null && !insurance.trim().isEmpty()) {
                    if (provider.getInsuranceAccepted() == null || provider.getInsuranceAccepted().isEmpty()) {
                        return false;
                    }
                    boolean hasInsurance = provider.getInsuranceAccepted().stream()
                        .anyMatch(ins -> ins.equalsIgnoreCase(insurance));
                    if (!hasInsurance) return false;
                }
                
                // Filter by consultation fee range
                if (provider.getConsultationFee() != null) {
                    if (minFee != null && provider.getConsultationFee() < minFee) {
                        return false;
                    }
                    if (maxFee != null && provider.getConsultationFee() > maxFee) {
                        return false;
                    }
                }
                
                // Filter by rating (mock rating for now - will be real rating later)
                if (minRating != null && minRating > 0) {
                    // Generate a mock rating based on provider ID for consistency
                    double mockRating = 3.5 + (provider.getId() % 15) * 0.1; // Ratings between 3.5-5.0
                    if (mockRating < minRating) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    @Override
    public Map<String, Object> getProviderProfileByUsername(String username) {
        HealthcareProvider provider = providerRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", provider.getId());
        profile.put("firstName", provider.getFirstName());
        profile.put("lastName", provider.getLastName());
        profile.put("email", provider.getEmail());
        profile.put("city", provider.getCity());
        profile.put("specializations", provider.getSpecializations());
        profile.put("insuranceAccepted", provider.getInsuranceAccepted());
        profile.put("consultationFee", provider.getConsultationFee());
        profile.put("dateOfBirth", provider.getDateOfBirth());
        profile.put("role", provider.getRole());
        return profile;
    }

    @Override
    @Transactional
    public Map<String, Object> updateProviderProfileByUsername(String username, HealthcareProvider updatedData) {
        HealthcareProvider provider = providerRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Healthcare provider not found"));

        if (updatedData.getFirstName() != null)
            provider.setFirstName(updatedData.getFirstName());
        if (updatedData.getLastName() != null)
            provider.setLastName(updatedData.getLastName());
        if (updatedData.getCity() != null)
            provider.setCity(updatedData.getCity());
        if (updatedData.getConsultationFee() != null)
            provider.setConsultationFee(updatedData.getConsultationFee());
        if (updatedData.getInsuranceAccepted() != null)
            provider.setInsuranceAccepted(updatedData.getInsuranceAccepted());
        if (updatedData.getSpecializations() != null)
            provider.setSpecializations(updatedData.getSpecializations());

        providerRepo.save(provider);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Provider profile updated successfully");
        response.put("updatedProvider", provider);
        return response;
    }

}
