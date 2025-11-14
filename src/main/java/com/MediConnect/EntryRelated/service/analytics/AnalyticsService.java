package com.MediConnect.EntryRelated.service.analytics;

import java.util.Map;

/**
 * Service interface for doctor analytics
 * 
 * Provides analytics data including:
 * - Profile views
 * - Post interactions (likes, comments)
 * - Appointment statistics
 * - Patient engagement metrics
 */
public interface AnalyticsService {
    /**
     * Get comprehensive analytics for a doctor
     * 
     * @param doctorId The ID of the doctor
     * @return Map containing all analytics data
     */
    Map<String, Object> getDoctorAnalytics(Long doctorId);
}

