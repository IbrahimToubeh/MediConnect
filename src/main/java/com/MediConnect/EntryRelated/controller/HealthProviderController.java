package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.healthprovider.LoginHPRequestDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.service.healthprovider.HealthProviderService;
import com.MediConnect.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/healthprovider")
public class HealthProviderController {

    private final HealthProviderService healthProviderService;
    private final UserService userService;

    @PostMapping("/login")
    public String login(@RequestBody LoginHPRequestDTO healthProviderInfo) {
        return userService.verify(healthProviderInfo.getUsername(), healthProviderInfo.getPassword());
    }

    @PostMapping("/register")
    public String register(@RequestBody SignupHPRequestDTO healthProviderInfo) {
        return healthProviderService.register(healthProviderInfo);
    }
}