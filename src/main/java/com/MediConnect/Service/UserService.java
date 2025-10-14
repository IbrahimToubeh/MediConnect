package com.MediConnect.Service;

import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.Repos.UserRepo;
import com.MediConnect.config.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final JWTService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;

    public String authenticate(String username, String password) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        if (auth.isAuthenticated()) {
            return jwtService.generateToken((UserDetails) auth.getPrincipal());
        }
        throw new RuntimeException("Invalid username or password");
    }

    public void resetUserPassword(String email, String newPassword) {
        Users user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    public Users getUserByEmail(String email) {
        return userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void registerUser(Users user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Set new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    public Users getUserByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void enableTwoFactor(String username) {
        Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTwoFactorEnabled(true);
        userRepo.save(user);
    }

    public void disableTwoFactor(String username) {
        Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTwoFactorEnabled(false);
        userRepo.save(user);
    }

    public boolean isTwoFactorEnabled(String username) {
        Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled();
    }
}
