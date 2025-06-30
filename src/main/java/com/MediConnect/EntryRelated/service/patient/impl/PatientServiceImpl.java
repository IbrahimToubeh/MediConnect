package com.MediConnect.EntryRelated.service.patient.impl;

import com.MediConnect.EntryRelated.dto.patient.SignupPatientRequestDTO;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.service.OTPService;
import com.MediConnect.EntryRelated.service.patient.PatientService;
import com.MediConnect.EntryRelated.service.patient.mapper.PatientMapper;
import com.MediConnect.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepo patientRepo;
    private final UserService userService;
    private final OTPService otpService;
    private final PatientMapper patientMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public String register(SignupPatientRequestDTO dto) {
        String normalizedEmail = dto.getEmail().trim().toLowerCase();

        if (patientRepo.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (patientRepo.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already exists");
        }

        Patient patient = patientMapper.signupDtoToPatient(dto);
        patient.setEmail(normalizedEmail);
        patient.setRegistrationDate(new Date());
        patient.setPassword(passwordEncoder.encode(patient.getPassword()));

        patientRepo.save(patient);
        otpService.clearRegistrationOTP(normalizedEmail);

        return "Patient registered successfully";
    }

    public Patient getPatientProfile(Long id) {
        return patientRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

}
