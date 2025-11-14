package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.admin.AdminLoginRequestDTO;
import com.MediConnect.EntryRelated.dto.admin.AdminLoginResponseDTO;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.Repos.UserRepo;
import com.MediConnect.Service.UserService;
import com.MediConnect.EntryRelated.exception.AccountStatusException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;
    private final UserRepo userRepo;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponseDTO> login(@Valid @RequestBody AdminLoginRequestDTO request) {
        String username = request.getUsername().trim();
        Users admin = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            log.warn("Unauthorized login attempt for non-admin user '{}'", username);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        try {
            String token = userService.authenticate(username, request.getPassword());
            AdminLoginResponseDTO response = new AdminLoginResponseDTO("success", token, admin.getUsername(), "ADMIN");
            return ResponseEntity.ok(response);
        } catch (AccountStatusException ex) {
            log.warn("Admin account '{}' blocked due to status {}", username, ex.getAccountStatus());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (BadCredentialsException ex) {
            log.warn("Invalid credentials provided for admin login attempt '{}'", username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> profile(Authentication authentication) {
        String username = authentication.getName();
        Users admin = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("username", admin.getUsername());
        profile.put("email", admin.getEmail());
        profile.put("firstName", admin.getFirstName());
        profile.put("lastName", admin.getLastName());
        profile.put("role", "ADMIN");

        return ResponseEntity.ok(profile);
    }
}

