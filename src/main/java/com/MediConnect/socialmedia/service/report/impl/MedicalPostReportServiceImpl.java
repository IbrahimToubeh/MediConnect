package com.MediConnect.socialmedia.service.report.impl;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.entity.MedicalPostReport;
import com.MediConnect.socialmedia.entity.NotificationType;
import com.MediConnect.socialmedia.entity.enums.PostReportReason;
import com.MediConnect.socialmedia.entity.enums.PostReporterType;
import com.MediConnect.socialmedia.repository.MedicalPostReportRepository;
import com.MediConnect.socialmedia.repository.MedicalPostRepository;
import com.MediConnect.socialmedia.service.NotificationService;
import com.MediConnect.socialmedia.service.report.MedicalPostReportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicalPostReportServiceImpl implements MedicalPostReportService {

    private final MedicalPostRepository medicalPostRepository;
    private final MedicalPostReportRepository medicalPostReportRepository;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final PatientRepo patientRepo;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void submitReport(Long postId,
                             Long reporterId,
                             PostReporterType reporterType,
                             PostReportReason reason,
                             String otherReason,
                             String details) {

        MedicalPost post = medicalPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));

        // Prevent duplicate reports from the same reporter on the same post
        if (medicalPostReportRepository.existsByPostIdAndReporterId(postId, reporterId)) {
            throw new IllegalStateException("You have already reported this post.");
        }

        Users reporter = resolveReporter(reporterId, reporterType);

        MedicalPostReport report = new MedicalPostReport();
        report.setPost(post);
        report.setReporterId(reporterId);
        report.setReporterType(reporterType);
        report.setReason(reason);
        report.setOtherReason(reason == PostReportReason.OTHER ? normalize(otherReason) : null);
        report.setDetails(normalize(details));

        medicalPostReportRepository.save(report);

        String reporterDisplay = formatReporterName(reporter, reporterType);
        String friendlyReason = reason != null ? reason.name().replace('_', ' ').toLowerCase() : "unspecified reason";
        String message = reporterDisplay + " reported post #" + postId + " for " + friendlyReason + ".";
        notificationService.createAdminNotification(reporter, message, NotificationType.ADMIN_POST_REPORTED, postId);
    }

    private Users resolveReporter(Long reporterId, PostReporterType reporterType) {
        return switch (reporterType) {
            case DOCTOR -> healthcareProviderRepo.findById(reporterId)
                    .orElseThrow(() -> new EntityNotFoundException("Doctor not found with id: " + reporterId));
            case PATIENT -> patientRepo.findById(reporterId)
                    .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + reporterId));
            default -> throw new IllegalArgumentException("Unsupported reporter type: " + reporterType);
        };
    }

    private String formatReporterName(Users reporter, PostReporterType reporterType) {
        if (reporterType == PostReporterType.DOCTOR && reporter instanceof HealthcareProvider doctor) {
            return "Dr. " + buildFullName(doctor.getFirstName(), doctor.getLastName());
        }
        if (reporter instanceof Patient patient) {
            return buildFullName(patient.getFirstName(), patient.getLastName());
        }
        return "A user";
    }

    private String buildFullName(String firstName, String lastName) {
        StringBuilder builder = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            builder.append(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(lastName.trim());
        }
        return builder.length() > 0 ? builder.toString() : "A user";
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

