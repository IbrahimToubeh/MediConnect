package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.ForgotPasswordRequestDTO;
import com.MediConnect.EntryRelated.dto.ResetPasswordRequestDTO;
import com.MediConnect.EntryRelated.dto.SendMailRequestDTO;
import com.MediConnect.EntryRelated.dto.VerifyOTPRequestDTO;
import com.MediConnect.EntryRelated.service.OTPService;
import com.MediConnect.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/otp")
public class OTPController {

    private final OTPService otpService;
    private final UserService userService;

    @PostMapping("/send-registration")
    public ResponseEntity<Map<String, String>> sendRegistrationOTP(@Valid @RequestBody SendMailRequestDTO emailRequest) {
        return buildSuccessResponse(otpService.sendRegistrationOTP(emailRequest.getEmail()));
    }

    @PostMapping("/verify-registration")
    public ResponseEntity<Map<String, String>> verifyRegistrationOTP(@Valid @RequestBody VerifyOTPRequestDTO otpRequest) {
        if (otpService.verifyRegistrationOTP(otpRequest.getEmail(), otpRequest.getOtp())) {
            otpService.clearRegistrationOTP(otpRequest.getEmail());
            return buildSuccessResponse("OTP verified successfully. You can now complete registration.");
        } else {
            return buildErrorResponse("Invalid or expired OTP");
        }
    }

    @PostMapping("/resend-registration")
    public ResponseEntity<Map<String, String>> resendRegistrationOTP(@Valid @RequestBody SendMailRequestDTO emailRequest) {
        otpService.sendRegistrationOTP(emailRequest.getEmail());
        return buildSuccessResponse("Registration OTP resent successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO forgotRequest) {
        try {
            userService.getUserByEmail(forgotRequest.getEmail());
        } catch (RuntimeException e) {
            return buildErrorResponse("No account found with this email address");
        }
        otpService.sendPasswordResetOTP(forgotRequest.getEmail());
        return buildSuccessResponse("Password reset OTP sent successfully");
    }

    @PostMapping("/verify-password-reset")
    public ResponseEntity<Map<String, String>> verifyResetOTP(@Valid @RequestBody VerifyOTPRequestDTO otpRequest) {
        if (otpService.verifyPasswordResetOTP(otpRequest.getEmail(), otpRequest.getOtp())) {
            return buildSuccessResponse("OTP verified successfully. You can now reset your password.");
        } else {
            return buildErrorResponse("Invalid or expired OTP");
        }
    }

    @PostMapping("/resend-password-reset")
    public ResponseEntity<Map<String, String>> resendResetOTP(@Valid @RequestBody ForgotPasswordRequestDTO emailRequest) {
        otpService.sendPasswordResetOTP(emailRequest.getEmail());
        return buildSuccessResponse("Password reset OTP resent successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO resetRequest) {
        if (!resetRequest.getNewPassword().equals(resetRequest.getConfirmPassword())) {
            return buildErrorResponse("New password and confirm password do not match");
        }
        if (!otpService.verifyPasswordResetOTP(resetRequest.getEmail(), resetRequest.getOtp())) {
            return buildErrorResponse("Invalid or expired OTP");
        }
        userService.resetUserPassword(resetRequest.getEmail(), resetRequest.getNewPassword());
        otpService.clearPasswordResetOTP(resetRequest.getEmail());
        return buildSuccessResponse("Password reset successfully. Please login with your new password.");
    }

    private ResponseEntity<Map<String, String>> buildSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }
}
