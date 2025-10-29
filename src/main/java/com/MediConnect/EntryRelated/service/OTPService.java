package com.MediConnect.EntryRelated.service;

import com.MediConnect.Service.OtpCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class OTPService {

    private final JavaMailSender mailSender;
    private final OtpCacheService otpCacheService;

    public String sendRegistrationOTP(String email) {
        String cleanedEmail = normalize(email);
        validateEmail(cleanedEmail);
        String otp = generateOTP();
        otpCacheService.cacheOTP(cleanedEmail + "_REGISTRATION", otp);
        sendOTPEmail(cleanedEmail, otp, "Registration");
        return "Registration OTP sent successfully";
    }

    public String sendPasswordResetOTP(String email) {
        String cleanedEmail = normalize(email);
        validateEmail(cleanedEmail);
        String otp = generateOTP();
        otpCacheService.cacheOTP(cleanedEmail + "_PASSWORD_RESET", otp);
        sendOTPEmail(cleanedEmail, otp, "Password Reset");
        return "Password reset OTP sent successfully";
    }

    public boolean verifyRegistrationOTP(String email, String otp) {
        String cleanedEmail = normalize(email);
        String cached = otpCacheService.getCachedOTP(cleanedEmail + "_REGISTRATION");
        return otp != null && otp.equals(cached);
    }

    public boolean verifyPasswordResetOTP(String email, String otp) {
        String cleanedEmail = normalize(email);
        String cached = otpCacheService.getCachedOTP(cleanedEmail + "_PASSWORD_RESET");
        return otp != null && otp.equals(cached);
    }

    public void clearRegistrationOTP(String email) {
        otpCacheService.clearOTP(normalize(email) + "_REGISTRATION");
    }

    public void clearPasswordResetOTP(String email) {
        otpCacheService.clearOTP(normalize(email) + "_PASSWORD_RESET");
    }

    public void markRegistrationOTPAsVerified(String email) {
        otpCacheService.cacheOTP(normalize(email) + "_REGISTRATION_VERIFIED", "true");
    }

    public boolean isRegistrationOTPVerified(String email) {
        String verified = otpCacheService.getCachedOTP(normalize(email) + "_REGISTRATION_VERIFIED");
        return "true".equals(verified);
    }

    public String sendLoginOTP(String email) {
        String cleanedEmail = normalize(email);
        validateEmail(cleanedEmail);
        String otp = generateOTP();
        otpCacheService.cacheOTP(cleanedEmail + "_LOGIN", otp);
        sendOTPEmail(cleanedEmail, otp, "Login Verification");
        return "Login OTP sent successfully";
    }

    public boolean verifyLoginOTP(String email, String otp) {
        String cleanedEmail = normalize(email);
        String cached = otpCacheService.getCachedOTP(cleanedEmail + "_LOGIN");
        return otp != null && otp.equals(cached);
    }

    public void clearLoginOTP(String email) {
        otpCacheService.clearOTP(normalize(email) + "_LOGIN");
    }

    private void sendOTPEmail(String email, String otp, String purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("MediConnect " + purpose + " OTP");
        message.setText("Your " + purpose + " OTP is: " + otp + ". Valid for 5 minutes.");
        mailSender.send(message);
    }

    private String generateOTP() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private void validateEmail(String email) {
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Invalid email address");
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
