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
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final AuthenticationManager authenticationManager;

    public Users registerUser(Users user) {
        if (userRepo.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));

        return userRepo.save(user);
    }

    public String verify(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            if (authentication.isAuthenticated()) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                return jwtService.generateToken(userDetails);
            }
        } catch (Exception e) {
            return "Authentication failed: " + e.getMessage();
        }
        return "Authentication failed";
    }

    // Method to verify user with specific role
    public String verifyWithRole(String username, String password, String expectedRole) {
        try {
            Users user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getRole().equals(expectedRole)) {
                return "Invalid role for this login endpoint";
            }

            return verify(username, password);
        } catch (Exception e) {
            return "Authentication failed: " + e.getMessage();
        }
    }
}