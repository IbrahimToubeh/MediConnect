package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.patient.LoginPatientRequestDTO;
import com.MediConnect.EntryRelated.dto.patient.SignupPatientRequestDTO;
import com.MediConnect.EntryRelated.entities.LaboratoryResult;
import com.MediConnect.EntryRelated.repository.LabResultRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.EntryRelated.service.patient.PatientService;
import com.MediConnect.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final UserService userService;
    private final PatientRepo patientRepo;
    private final LabResultRepo labResultRepo;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody SignupPatientRequestDTO patientInfo) {
        try {
            String result = patientService.register(patientInfo);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginPatientRequestDTO patientInfo) {
        try {
            String token = userService.authenticate(patientInfo.getUsername(), patientInfo.getPassword());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Login successful");
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/upload-lab-result")
    public ResponseEntity<Map<String, String>> uploadLabResult(
            @RequestParam("patientId") Long patientId,
            @RequestParam("description") String description,
            @RequestParam("image") MultipartFile imageFile
    ) {
        try {
            if (imageFile.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Image file is required");
                return ResponseEntity.badRequest().body(response);
            }

            LaboratoryResult result = new LaboratoryResult();
            result.setDescription(description);
            result.setPatient(patientRepo.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found")));
            result.setImage(imageFile.getBytes());
            labResultRepo.save(result);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Lab result uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Invalid image file");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
