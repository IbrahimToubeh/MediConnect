package com.MediConnect.EntryRelated.service.notification;

import com.MediConnect.EntryRelated.entities.AccountStatus;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends transactional emails to healthcare providers whenever their account status changes.
 */
@Service
@RequiredArgsConstructor
public class DoctorAccountNotificationService {

    private final JavaMailSender mailSender;

    public void sendStatusChangeEmail(HealthcareProvider provider, AccountStatus status) {
        if (provider == null || !StringUtils.hasText(provider.getEmail()) || status == null) {
            return;
        }

        String subject;
        String body;

        switch (status) {
            case ACTIVE -> {
                subject = "MediConnect · Your doctor account has been approved";
                body = """
                        Hello %s %s,

                        Great news! Your MediConnect doctor account has been reviewed and approved. You can now sign in, complete your profile, and start engaging with patients.

                        If you did not initiate this registration, please contact our support team immediately.

                        Regards,
                        MediConnect Support
                        """.formatted(defaultString(provider.getFirstName()), defaultString(provider.getLastName()));
            }
            case REJECTED -> {
                subject = "MediConnect · Your doctor registration was not approved";
                body = """
                        Hello %s %s,

                        Thank you for applying to join MediConnect. After reviewing your submission we were unable to approve the account at this time.

                        You are welcome to submit a new application with updated information. If you believe this decision was made in error, please reach out to our support team.

                        Regards,
                        MediConnect Support
                        """.formatted(defaultString(provider.getFirstName()), defaultString(provider.getLastName()));
            }
            default -> {
                subject = null;
                body = null;
            }
        }

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(body)) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(provider.getEmail());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String defaultString(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}


