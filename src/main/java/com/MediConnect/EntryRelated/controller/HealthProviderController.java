package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.healthprovider.GetAllSpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.LoginHPRequestDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.service.healthprovider.HealthcareProviderService;
import com.MediConnect.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/healthprovider")
public class HealthProviderController {

    private final HealthcareProviderService healthcareProviderService;
    private final UserService userService;

    @PostMapping("/login")
    public String login(@RequestBody LoginHPRequestDTO healthProviderInfo) {
        return userService.authenticate(healthProviderInfo.getUsername(), healthProviderInfo.getPassword());
    }

    @PostMapping("/register")
    public String register(@RequestBody SignupHPRequestDTO healthProviderInfo) {
        return healthcareProviderService.register(healthProviderInfo);
    }
    @GetMapping("GetAllSpecialty")
    public List<GetAllSpecialtyDTO> GetAllSpecialty() {
        return healthcareProviderService.getAllSpecialtyDTO();
    }
}