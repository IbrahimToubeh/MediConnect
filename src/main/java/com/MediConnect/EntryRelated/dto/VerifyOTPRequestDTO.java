package com.MediConnect.EntryRelated.dto;

import lombok.Getter;

@Getter
public class VerifyOTPRequestDTO {
    private String email;
    private String otp;
}