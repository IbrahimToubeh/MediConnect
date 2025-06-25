package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.patient.LoginPatientRequestDTO;
import com.MediConnect.EntryRelated.dto.patient.SignupPatientRequestDTO;
import com.MediConnect.EntryRelated.entities.LaboratoryResult;
import com.MediConnect.EntryRelated.repository.LabResultRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.service.patient.PatientService;
import com.MediConnect.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final UserService userService;
    private final PatientRepo patientRepo;
    private final LabResultRepo labResultRepo;

    @PostMapping("/login")
    public String login(@RequestBody LoginPatientRequestDTO patientInfo) {
        return userService.verify(patientInfo.getUsername(), patientInfo.getPassword());
    }

    @PostMapping("/register")
    public String register(@RequestBody SignupPatientRequestDTO patientInfo) {
        return patientService.register(patientInfo);
    }

    @PostMapping("/upload-lab-result")
    public ResponseEntity<String> uploadLabResult(
            @RequestParam("patientId") Long patientId,
            @RequestParam("description") String description,
            @RequestParam("image") MultipartFile imageFile
    ) {
        LaboratoryResult result = new LaboratoryResult();
        result.setDescription(description);
        result.setPatient(patientRepo.findById(patientId).orElseThrow());

        try {
            result.setImage(imageFile.getBytes());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid image file");
        }

        labResultRepo.save(result);
        return ResponseEntity.ok("Lab result uploaded");
    }
}
