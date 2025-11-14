package com.MediConnect.config;

import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.Repos.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Ensures a default administrator account exists based on configuration.
 * Designed for development bootstrap; update credentials for production use.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepo userRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${admin.default.enabled:false}")
    private boolean adminBootstrapEnabled;

    @Value("${admin.default.username:}")
    private String defaultAdminUsername;

    @Value("${admin.default.password:}")
    private String defaultAdminPassword;

    @Value("${admin.default.email:}")
    private String defaultAdminEmail;

    @Value("${admin.default.first-name:Admin}")
    private String defaultAdminFirstName;

    @Value("${admin.default.last-name:User}")
    private String defaultAdminLastName;

    @Override
    public void run(String... args) {
        if (!adminBootstrapEnabled) {
            log.info("Default admin bootstrap disabled. Skipping admin user initialization.");
            return;
        }

        if (isBlank(defaultAdminUsername) || isBlank(defaultAdminPassword) || isBlank(defaultAdminEmail)) {
            log.warn("Default admin configuration incomplete. Please provide username, password, and email.");
            return;
        }

        userRepo.findByUsername(defaultAdminUsername.trim())
                .ifPresentOrElse(existing -> {
                    if (!"ADMIN".equalsIgnoreCase(existing.getRole())) {
                        existing.setRole("ADMIN");
                        userRepo.save(existing);
                        log.info("Existing user '{}' promoted to ADMIN role.", defaultAdminUsername);
                    } else {
                        log.info("Admin user '{}' already exists.", defaultAdminUsername);
                    }
                }, () -> {
                    Users admin = new Users();
                    admin.setUsername(defaultAdminUsername.trim());
                    admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
                    admin.setRole("ADMIN");
                    admin.setFirstName(defaultAdminFirstName);
                    admin.setLastName(defaultAdminLastName);
                    admin.setEmail(defaultAdminEmail.trim().toLowerCase());
                    admin.setRegistrationDate(new Date());
                    admin.setTwoFactorEnabled(false);
                    userRepo.save(admin);
                    log.info("Default admin user '{}' created.", defaultAdminUsername);
                });
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

