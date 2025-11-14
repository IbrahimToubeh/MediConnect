package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.admin.UpdateAccountFlagRequest;
import com.MediConnect.EntryRelated.dto.admin.UpdateAccountStatusRequest;
import com.MediConnect.EntryRelated.entities.AccountStatus;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/patients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminPatientController {

    private final PatientRepo patientRepo;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPatients(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "includeFlagged", required = false, defaultValue = "false") boolean includeFlagged
    ) {

        List<Patient> patients = patientRepo.findAll();

        String normalizedName = StringUtils.hasText(name) ? name.trim().toLowerCase(Locale.ROOT) : null;
        String normalizedCity = StringUtils.hasText(city) ? city.trim().toLowerCase(Locale.ROOT) : null;

        AccountStatus statusFilter = null;
        if (StringUtils.hasText(status)) {
            try {
                statusFilter = AccountStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring unknown status filter value: {}", status);
            }
        }
        final AccountStatus finalStatusFilter = statusFilter;

        List<Map<String, Object>> summaries = patients.stream()
                .filter(patient -> {
                    if (normalizedName != null) {
                        String fullName = (patient.getFirstName() + " " + patient.getLastName()).toLowerCase(Locale.ROOT);
                        if (!fullName.contains(normalizedName)) {
                            return false;
                        }
                    }

                    if (normalizedCity != null) {
                        String patientCity = patient.getCity() != null ? patient.getCity().toLowerCase(Locale.ROOT) : "";
                        if (!patientCity.contains(normalizedCity)) {
                            return false;
                        }
                    }

                    if (finalStatusFilter != null) {
                        AccountStatus patientStatus = Optional.ofNullable(patient.getAccountStatus()).orElse(AccountStatus.ACTIVE);
                        if (patientStatus != finalStatusFilter) {
                            return false;
                        }
                    }

                    if (!includeFlagged && Boolean.TRUE.equals(patient.getAdminFlagged())) {
                        return false;
                    }

                    return true;
                })
                .map(this::mapPatientToSummary)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "count", summaries.size(),
                "patients", summaries
        ));
    }

    private Map<String, Object> mapPatientToSummary(Patient patient) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", patient.getId());
        summary.put("firstName", patient.getFirstName());
        summary.put("lastName", patient.getLastName());
        summary.put("email", patient.getEmail());
        summary.put("phoneNumber", patient.getPhoneNumber());
        summary.put("city", patient.getCity());
        summary.put("country", patient.getCountry());
        summary.put("insuranceProvider", patient.getInsuranceProvider());
        summary.put("insuranceNumber", patient.getInsuranceNumber());
        summary.put("registrationDate", patient.getRegistrationDate());
        summary.put("accountStatus", Optional.ofNullable(patient.getAccountStatus()).orElse(AccountStatus.ACTIVE).name());
        summary.put("adminFlagged", Boolean.TRUE.equals(patient.getAdminFlagged()));
        summary.put("adminFlagReason", patient.getAdminFlagReason());
        summary.put("adminFlaggedAt", patient.getAdminFlaggedAt());
        return summary;
    }

    @PostMapping("/{patientId}/flag")
    public ResponseEntity<Map<String, Object>> flagPatient(
            @PathVariable Long patientId,
            @RequestBody UpdateAccountFlagRequest request) {

        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with id " + patientId));

        String reason = Optional.ofNullable(request.getReason()).map(String::trim).orElse("");
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("Please provide a reason when flagging a patient account.");
        }

        patient.setAdminFlagged(true);
        patient.setAdminFlagReason(reason);
        patient.setAdminFlaggedAt(new Date());
        patientRepo.save(patient);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Patient account flagged for review.",
                "adminFlagged", true,
                "adminFlagReason", reason,
                "adminFlaggedAt", patient.getAdminFlaggedAt()
        ));
    }

    @DeleteMapping("/{patientId}/flag")
    public ResponseEntity<Map<String, Object>> unflagPatient(@PathVariable Long patientId) {
        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with id " + patientId));

        if (!Boolean.TRUE.equals(patient.getAdminFlagged())) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Patient account was not flagged.",
                    "adminFlagged", false
            ));
        }

        patient.setAdminFlagged(false);
        patient.setAdminFlagReason(null);
        patient.setAdminFlaggedAt(null);
        patientRepo.save(patient);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Patient account flag removed.",
                "adminFlagged", false
        ));
    }

    @PatchMapping("/{patientId}/status")
    public ResponseEntity<Map<String, Object>> updatePatientStatus(
            @PathVariable Long patientId,
            @RequestBody UpdateAccountStatusRequest request) {

        try {
            Patient patient = patientRepo.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found with id " + patientId));

            AccountStatus newStatus;
            try {
                newStatus = AccountStatus.valueOf(request.getStatus().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unsupported status value: " + request.getStatus());
            }

            AccountStatus previousStatus = patient.getAccountStatus();
            if (previousStatus == AccountStatus.BANNED && newStatus != AccountStatus.BANNED) {
                throw new IllegalStateException("Banned accounts cannot be reinstated. Please review the ban policy.");
            }
            patient.setAccountStatus(newStatus);
            patientRepo.save(patient);

            String message;
            switch (newStatus) {
                case ON_HOLD -> message = "Patient account is now on hold.";
                case BANNED -> message = "Patient account has been banned permanently.";
                case ACTIVE -> message = "Patient account has been reactivated.";
                default -> message = "Patient status updated.";
            }

            log.info("Admin updated patient {} status from {} to {}", patientId, previousStatus, newStatus);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", message,
                    "accountStatus", newStatus.name()
            ));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            log.warn("Failed to update patient {} status: {}", patientId, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        }
    }
}

