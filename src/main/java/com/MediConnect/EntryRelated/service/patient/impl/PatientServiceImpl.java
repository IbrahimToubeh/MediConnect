package com.MediConnect.EntryRelated.service.patient.impl;

import com.MediConnect.EntryRelated.dto.patient.SignupPatientRequestDTO;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.service.patient.mapper.PatientMapper;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.service.patient.PatientService;
import com.MediConnect.EntryRelated.service.patient.mapper.PatientMapper;
import com.MediConnect.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepo patientRepo;
    private final UserService userService;
    private final PatientMapper patientMapper;

    @Override
    @Transactional
    public String register(SignupPatientRequestDTO signupPatientRequestDTO) {
        try {
            if (patientRepo.existsByUsername(signupPatientRequestDTO.getUsername())) {
                return "Username already exists";
            }
            if (patientRepo.existsByEmail(signupPatientRequestDTO.getEmail())) {
                return "Email already exists";
            }

            Patient patient = patientMapper.signupDtoToPatient(signupPatientRequestDTO);

            userService.registerUser(patient);
            return "Patient registered successfully";

        } catch (Exception e) {
            return "Registration failed: " + e.getMessage();
        }
    }
}