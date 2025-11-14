package com.MediConnect.socialmedia.service.report;

import com.MediConnect.socialmedia.entity.enums.PostReportReason;
import com.MediConnect.socialmedia.entity.enums.PostReporterType;

public interface MedicalPostReportService {
    void submitReport(Long postId,
                      Long reporterId,
                      PostReporterType reporterType,
                      PostReportReason reason,
                      String otherReason,
                      String details);
}

