package com.MediConnect.EntryRelated.controller;

import com.MediConnect.EntryRelated.dto.admin.UpdateAccountFlagRequest;
import com.MediConnect.EntryRelated.dto.admin.UpdateAccountStatusRequest;
import com.MediConnect.EntryRelated.entities.AccountStatus;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.SpecializationType;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.service.notification.DoctorAccountNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/doctors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminDoctorController {

    private final HealthcareProviderRepo healthcareProviderRepo;
    private final DoctorAccountNotificationService notificationService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDoctors(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "specialty", required = false) String specialty,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "includeFlagged", required = false, defaultValue = "false") boolean includeFlagged
    ) {

        List<HealthcareProvider> providers = healthcareProviderRepo.findAll();

        String normalizedName = StringUtils.hasText(name) ? name.trim().toLowerCase(Locale.ROOT) : null;
        String normalizedCity = StringUtils.hasText(city) ? city.trim().toLowerCase(Locale.ROOT) : null;

        SpecializationType parsedSpecialization = null;
        if (StringUtils.hasText(specialty)) {
            try {
                parsedSpecialization = SpecializationType.valueOf(specialty.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring unknown specialty filter value: {}", specialty);
            }
        }
        final SpecializationType specializationFilter = parsedSpecialization;
        AccountStatus statusFilter = null;
        if (StringUtils.hasText(status)) {
            try {
                statusFilter = AccountStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring unknown status filter value: {}", status);
            }
        }
        final AccountStatus finalStatusFilter = statusFilter;

        List<Map<String, Object>> doctors = providers.stream()
                .filter(provider -> {
                    if (normalizedName != null) {
                        String fullName = (provider.getFirstName() + " " + provider.getLastName()).toLowerCase(Locale.ROOT);
                        String clinicName = provider.getClinicName() != null ? provider.getClinicName().toLowerCase(Locale.ROOT) : "";
                        if (!fullName.contains(normalizedName) && !clinicName.contains(normalizedName)) {
                            return false;
                        }
                    }

                    if (normalizedCity != null) {
                        String providerCity = provider.getCity() != null ? provider.getCity().toLowerCase(Locale.ROOT) : "";
                        if (!providerCity.contains(normalizedCity)) {
                            return false;
                        }
                    }

                    if (specializationFilter != null) {
                        List<SpecializationType> specializations = provider.getSpecializations();
                        if (specializations == null || specializations.stream().noneMatch(spec -> spec == specializationFilter)) {
                            return false;
                        }
                    }

                    if (finalStatusFilter != null) {
                        AccountStatus providerStatus = Optional.ofNullable(provider.getAccountStatus()).orElse(AccountStatus.ACTIVE);
                        if (providerStatus != finalStatusFilter) {
                            return false;
                        }
                    }

                    if (!includeFlagged && Boolean.TRUE.equals(provider.getAdminFlagged())) {
                        return false;
                    }

                    return true;
                })
                .map(this::mapProviderToSummary)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("count", doctors.size());
        response.put("doctors", doctors);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> mapProviderToSummary(HealthcareProvider provider) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", provider.getId());
        summary.put("firstName", provider.getFirstName());
        summary.put("lastName", provider.getLastName());
        summary.put("accountStatus", Optional.ofNullable(provider.getAccountStatus()).orElse(AccountStatus.ACTIVE).name());
        summary.put("adminFlagged", Boolean.TRUE.equals(provider.getAdminFlagged()));
        summary.put("adminFlagReason", provider.getAdminFlagReason());
        summary.put("adminFlaggedAt", provider.getAdminFlaggedAt());
        summary.put("email", provider.getEmail());
        summary.put("phoneNumber", provider.getPhoneNumber());
        summary.put("clinicName", provider.getClinicName());
        summary.put("bio", provider.getBio());
        summary.put("licenseNumber", provider.getLicenseNumber());
        summary.put("city", provider.getCity());
        summary.put("country", provider.getCountry());
        summary.put("address", provider.getAddress());
        summary.put("state", provider.getState());
        summary.put("zipcode", provider.getZipcode());
        summary.put("gender", provider.getGender());
        summary.put("dateOfBirth", provider.getDateOfBirth());
        summary.put("consultationFee", provider.getConsultationFee());
        summary.put("profilePicture", provider.getProfilePicture());
        summary.put("insuranceAccepted", provider.getInsuranceAccepted());
        summary.put("registrationDate", provider.getRegistrationDate());
        summary.put("availableDays", provider.getAvailableDays());
        summary.put("availableTimeStart", provider.getAvailableTimeStart());
        summary.put("availableTimeEnd", provider.getAvailableTimeEnd());

        List<SpecializationType> specializations = provider.getSpecializations();
        if (specializations != null) {
            List<String> specializationNames = specializations.stream()
                    .map(SpecializationType::name)
                    .collect(Collectors.toList());
            summary.put("specializations", specializationNames);
        } else {
            summary.put("specializations", Collections.emptyList());
        }

        List<Map<String, Object>> educationSummaries = Optional.ofNullable(provider.getEducationHistories())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::mapEducationHistory)
                .collect(Collectors.toList());
        summary.put("educationHistories", educationSummaries);

        List<Map<String, Object>> workSummaries = Optional.ofNullable(provider.getWorkExperiences())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::mapWorkExperience)
                .collect(Collectors.toList());
        summary.put("workExperiences", workSummaries);

        return summary;
    }

    private Map<String, Object> mapEducationHistory(EducationHistory history) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("institutionName", history.getInstitutionName());
        payload.put("startDate", history.getStartDate());
        payload.put("endDate", history.getEndDate());
        payload.put("stillEnrolled", history.isStillEnrolled());
        return payload;
    }

    private Map<String, Object> mapWorkExperience(WorkExperience experience) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("organizationName", experience.getOrganizationName());
        payload.put("roleTitle", experience.getRoleTitle());
        payload.put("startDate", experience.getStartDate());
        payload.put("endDate", experience.getEndDate());
        payload.put("stillWorking", experience.isStillWorking());
        return payload;
    }

    @PostMapping("/{doctorId}/flag")
    public ResponseEntity<Map<String, Object>> flagDoctor(
            @PathVariable Long doctorId,
            @RequestBody UpdateAccountFlagRequest request) {

        HealthcareProvider provider = healthcareProviderRepo.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id " + doctorId));

        String reason = Optional.ofNullable(request.getReason()).map(String::trim).orElse("");
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("Please provide a reason when flagging a doctor account.");
        }

        provider.setAdminFlagged(true);
        provider.setAdminFlagReason(reason);
        provider.setAdminFlaggedAt(new Date());
        healthcareProviderRepo.save(provider);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Doctor account flagged for review.");
        response.put("adminFlagged", true);
        response.put("adminFlagReason", reason);
        response.put("adminFlaggedAt", provider.getAdminFlaggedAt());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{doctorId}/flag")
    public ResponseEntity<Map<String, Object>> unflagDoctor(@PathVariable Long doctorId) {
        HealthcareProvider provider = healthcareProviderRepo.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id " + doctorId));

        if (!Boolean.TRUE.equals(provider.getAdminFlagged())) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Doctor account was not flagged.",
                    "adminFlagged", false
            ));
        }

        provider.setAdminFlagged(false);
        provider.setAdminFlagReason(null);
        provider.setAdminFlaggedAt(null);
        healthcareProviderRepo.save(provider);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Doctor account flag removed.",
                "adminFlagged", false
        ));
    }

    @PatchMapping("/{doctorId}/status")
    public ResponseEntity<Map<String, Object>> updateDoctorStatus(
            @PathVariable Long doctorId,
            @RequestBody UpdateAccountStatusRequest request) {

        try {
            HealthcareProvider provider = healthcareProviderRepo.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id " + doctorId));

            AccountStatus newStatus;
            try {
                newStatus = AccountStatus.valueOf(request.getStatus().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unsupported status value: " + request.getStatus());
            }

            AccountStatus previousStatus = Optional.ofNullable(provider.getAccountStatus()).orElse(AccountStatus.ACTIVE);

            if (previousStatus == AccountStatus.BANNED && newStatus != AccountStatus.BANNED) {
                throw new IllegalStateException("Banned accounts cannot be reinstated. Please review the ban policy.");
            }
            if (previousStatus == AccountStatus.REJECTED && newStatus != AccountStatus.REJECTED) {
                throw new IllegalStateException("Rejected registrations cannot be reactivated. Ask the doctor to submit a new application.");
            }
            if (newStatus == AccountStatus.REJECTED && previousStatus != AccountStatus.PENDING) {
                throw new IllegalStateException("Only pending registrations can be rejected.");
            }
            if (newStatus == AccountStatus.PENDING && previousStatus != AccountStatus.PENDING) {
                throw new IllegalStateException("Only newly registered doctors can be set to pending.");
            }

            provider.setAccountStatus(newStatus);
            healthcareProviderRepo.save(provider);

            boolean transitionedFromPending = previousStatus == AccountStatus.PENDING;

            String message;
            switch (newStatus) {
                case PENDING -> message = "Doctor account is awaiting verification.";
                case ON_HOLD -> message = "Doctor account is now on hold.";
                case BANNED -> message = "Doctor account has been banned permanently.";
                case REJECTED -> message = "Doctor registration has been rejected.";
                case ACTIVE -> message = "Doctor account has been reactivated.";
                default -> message = "Doctor status updated.";
            }

            log.info("Admin updated doctor {} status from {} to {}", doctorId, previousStatus, newStatus);

            if (transitionedFromPending && (newStatus == AccountStatus.ACTIVE || newStatus == AccountStatus.REJECTED)) {
                notificationService.sendStatusChangeEmail(provider, newStatus);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", message);
            response.put("accountStatus", newStatus.name());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            log.warn("Failed to update doctor {} status: {}", doctorId, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        }
    }
}

