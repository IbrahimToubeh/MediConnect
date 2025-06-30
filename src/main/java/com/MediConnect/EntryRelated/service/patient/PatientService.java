package com.MediConnect.EntryRelated.service.patient;

import com.MediConnect.EntryRelated.dto.patient.SignupPatientRequestDTO;
import org.springframework.stereotype.Component;

@Component
public interface PatientService {
    String register(SignupPatientRequestDTO signupPatientRequestDTO);
}
