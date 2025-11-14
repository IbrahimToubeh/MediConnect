package com.MediConnect.util;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.Repos.UserRepo;
import com.MediConnect.config.JWTService;
import com.MediConnect.socialmedia.entity.enums.PostReporterType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility class for extracting user information from JWT tokens.
 * 
 * This class centralizes the logic for:
 * - Parsing JWT tokens and extracting usernames
 * - Resolving usernames to user IDs by checking HealthcareProvider, Patient, and Users tables
 * - Determining user types (DOCTOR, PATIENT) for post-related operations
 * 
 * All methods handle the "Bearer " prefix automatically and provide consistent error handling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenExtractor {
    
    private final JWTService jwtService;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final PatientRepo patientRepo;
    private final UserRepo userRepo;
    
    /**
     * Extracts the user ID from a JWT token.
     * 
     * The method:
     * 1. Removes "Bearer " prefix if present
     * 2. Extracts username from the JWT token
     * 3. Searches for the user in HealthcareProvider, Patient, and Users tables (in that order)
     * 4. Returns the user's ID
     * 
     * @param token The JWT token (with or without "Bearer " prefix)
     * @return The user ID
     * @throws RuntimeException if the token is invalid or user is not found
     */
    public Long extractUserIdFromToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            String cleanToken = removeBearerPrefix(token);
            
            // Extract username from JWT token
            String username = jwtService.extractUserName(cleanToken);
            log.debug("Extracted username from token: {}", username);
            
            // Try to find healthcare provider first
            Optional<HealthcareProvider> provider = healthcareProviderRepo.findByUsername(username);
            if (provider.isPresent()) {
                Long userId = provider.get().getId();
                log.debug("Found healthcare provider with ID: {}", userId);
                return userId;
            }
            
            // If not a healthcare provider, try patient
            Optional<Patient> patient = patientRepo.findByUsername(username);
            if (patient.isPresent()) {
                Long userId = patient.get().getId();
                log.debug("Found patient with ID: {}", userId);
                return userId;
            }
            
            // Finally, check general users table (e.g., admin)
            Optional<Users> user = userRepo.findByUsername(username);
            if (user.isPresent()) {
                Long userId = user.get().getId();
                log.debug("Found user with ID: {}", userId);
                return userId;
            }
            
            // User not found in any table
            log.error("User not found for username: {}", username);
            throw new RuntimeException("User not found for username: " + username);
            
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract user ID from token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract user ID from token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts user context (ID and type) from a JWT token.
     * 
     * This method is used for post-related operations where the user type (DOCTOR/PATIENT)
     * is needed in addition to the user ID.
     * 
     * The method:
     * 1. Removes "Bearer " prefix if present
     * 2. Extracts username from the JWT token
     * 3. Searches for the user in HealthcareProvider, Patient, and Users tables (in that order)
     * 4. Returns a UserContext containing the user ID and PostReporterType
     * 
     * @param token The JWT token (with or without "Bearer " prefix)
     * @return UserContext containing user ID and PostReporterType
     * @throws RuntimeException if the token is invalid or user is not found
     */
    public UserContext extractUserContextFromToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            String cleanToken = removeBearerPrefix(token);
            
            // Extract username from JWT token
            String username = jwtService.extractUserName(cleanToken);
            log.debug("Extracting user context for username: {}", username);
            
            // Try to find healthcare provider first
            Optional<HealthcareProvider> providerOpt = healthcareProviderRepo.findByUsername(username);
            if (providerOpt.isPresent()) {
                Long userId = providerOpt.get().getId();
                log.debug("Found healthcare provider with ID: {}, type: DOCTOR", userId);
                return new UserContext(userId, PostReporterType.DOCTOR);
            }
            
            // If not a healthcare provider, try patient
            Optional<Patient> patientOpt = patientRepo.findByUsername(username);
            if (patientOpt.isPresent()) {
                Long userId = patientOpt.get().getId();
                log.debug("Found patient with ID: {}, type: PATIENT", userId);
                return new UserContext(patientOpt.get().getId(), PostReporterType.PATIENT);
            }
            
            // Finally, check general users table (e.g., admin)
            // Note: Users from the general table are treated as PATIENT type
            Optional<Users> userOpt = userRepo.findByUsername(username);
            if (userOpt.isPresent()) {
                Long userId = userOpt.get().getId();
                log.debug("Found user with ID: {}, type: PATIENT (from Users table)", userId);
                return new UserContext(userOpt.get().getId(), PostReporterType.PATIENT);
            }
            
            // User not found in any table
            log.error("User not found for username: {}", username);
            throw new RuntimeException("User not found for username: " + username);
            
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract user context from token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract user from token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Removes the "Bearer " prefix from a token if present.
     * 
     * @param token The token string (may or may not have "Bearer " prefix)
     * @return The token without the "Bearer " prefix
     */
    private String removeBearerPrefix(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
    
    /**
     * Record representing user context with ID and type.
     * Used for post-related operations where the user type is needed.
     */
    public record UserContext(Long id, PostReporterType type) {}
}

