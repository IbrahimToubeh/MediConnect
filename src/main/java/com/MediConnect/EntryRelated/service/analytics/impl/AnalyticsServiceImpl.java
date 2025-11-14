package com.MediConnect.EntryRelated.service.analytics.impl;

import com.MediConnect.Entities.AppointmentEntity;
import com.MediConnect.Entities.AppointmentStatus;
import com.MediConnect.EntryRelated.repository.AppointmentRepository;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.service.analytics.AnalyticsService;
import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.entity.MedicalPostComment;
import com.MediConnect.socialmedia.repository.MedicalPostCommentRepository;
import com.MediConnect.socialmedia.repository.MedicalPostLikeRepository;
import com.MediConnect.socialmedia.repository.MedicalPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of AnalyticsService
 * 
 * Calculates analytics data for doctors including:
 * - Profile views (mock data for now - can be enhanced with actual tracking)
 * - Post interactions (total likes, comments, engagement rate)
 * - Appointment statistics (total, pending, confirmed, completed, cancelled)
 * - Patient growth (new patients over time)
 * - Monthly trends (appointments and post engagement)
 */
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {
    
    private final MedicalPostRepository medicalPostRepository;
    private final MedicalPostLikeRepository medicalPostLikeRepository;
    private final MedicalPostCommentRepository medicalPostCommentRepository;
    private final AppointmentRepository appointmentRepository;
    private final HealthcareProviderRepo healthcareProviderRepo;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDoctorAnalytics(Long doctorId) {
        Map<String, Object> analytics = new HashMap<>();
        
        // Verify doctor exists
        if (!healthcareProviderRepo.existsById(doctorId)) {
            throw new RuntimeException("Doctor not found");
        }
        
        // 1. PROFILE VIEWS (mock data - can be enhanced with actual tracking)
        // TODO: Implement actual profile view tracking
        int profileViews = (int)(Math.random() * 500) + 100; // Mock: 100-600 views
        int profileViewsThisMonth = (int)(Math.random() * 100) + 20; // Mock: 20-120 this month
        int profileViewsLastMonth = (int)(Math.random() * 80) + 15; // Mock: 15-95 last month
        
        double profileViewsChange = profileViewsLastMonth > 0 
            ? ((double)(profileViewsThisMonth - profileViewsLastMonth) / profileViewsLastMonth) * 100 
            : 0;
        
        analytics.put("profileViews", profileViews);
        analytics.put("profileViewsThisMonth", profileViewsThisMonth);
        analytics.put("profileViewsLastMonth", profileViewsLastMonth);
        analytics.put("profileViewsChange", Math.round(profileViewsChange * 10.0) / 10.0);
        
        // 2. POST STATISTICS
        List<MedicalPost> doctorPosts = medicalPostRepository.findByPostProviderIdOrderByCreatedAtDesc(doctorId);
        int totalPosts = doctorPosts.size();
        
        // Calculate total likes and comments
        long totalLikes = 0;
        long totalComments = 0;
        List<Map<String, Object>> postEngagementData = new ArrayList<>();
        
        for (MedicalPost post : doctorPosts) {
            long postLikes = medicalPostLikeRepository.countByPostId(post.getId());
            List<MedicalPostComment> postComments = medicalPostCommentRepository.findByPostIdOrderByCreatedAtDesc(post.getId());
            int postCommentCount = postComments.size();
            
            totalLikes += postLikes;
            totalComments += postCommentCount;
            
            // Store individual post data for charts
            Map<String, Object> postData = new HashMap<>();
            postData.put("postId", post.getId());
            postData.put("likes", postLikes);
            postData.put("comments", postCommentCount);
            postData.put("engagement", postLikes + postCommentCount);
            postData.put("createdAt", post.getCreatedAt());
            postEngagementData.add(postData);
        }
        
        // Calculate engagement rate (average engagement per post)
        double avgEngagementPerPost = totalPosts > 0 
            ? (double)(totalLikes + totalComments) / totalPosts 
            : 0;
        
        // Calculate total engagement
        long totalEngagement = totalLikes + totalComments;
        
        analytics.put("totalPosts", totalPosts);
        analytics.put("totalLikes", totalLikes);
        analytics.put("totalComments", totalComments);
        analytics.put("totalEngagement", totalEngagement);
        analytics.put("avgEngagementPerPost", Math.round(avgEngagementPerPost * 10.0) / 10.0);
        analytics.put("postEngagementData", postEngagementData);
        
        // 3. APPOINTMENT STATISTICS
        List<AppointmentEntity> allAppointments = appointmentRepository.findByHealthcareProviderId(doctorId);
        int totalAppointments = allAppointments.size();
        
        // Count by status
        long pendingCount = allAppointments.stream()
            .filter(apt -> apt.getStatus() == AppointmentStatus.PENDING)
            .count();
        long confirmedCount = allAppointments.stream()
            .filter(apt -> apt.getStatus() == AppointmentStatus.CONFIRMED)
            .count();
        long completedCount = allAppointments.stream()
            .filter(apt -> apt.getStatus() == AppointmentStatus.COMPLETED)
            .count();
        long cancelledCount = allAppointments.stream()
            .filter(apt -> apt.getStatus() == AppointmentStatus.CANCELLED)
            .count();
        long rescheduledCount = allAppointments.stream()
            .filter(apt -> apt.getStatus() == AppointmentStatus.RESCHEDULED)
            .count();
        
        // Monthly appointment trends (last 6 months)
        List<Map<String, Object>> monthlyAppointments = getMonthlyAppointmentTrends(allAppointments, 6);
        
        analytics.put("totalAppointments", totalAppointments);
        analytics.put("pendingAppointments", (int)pendingCount);
        analytics.put("confirmedAppointments", (int)confirmedCount);
        analytics.put("completedAppointments", (int)completedCount);
        analytics.put("cancelledAppointments", (int)cancelledCount);
        analytics.put("rescheduledAppointments", (int)rescheduledCount);
        analytics.put("monthlyAppointmentTrends", monthlyAppointments);
        
        // 4. PATIENT GROWTH
        // Get unique patients
        Set<Long> uniquePatients = allAppointments.stream()
            .map(apt -> apt.getPatient().getId())
            .collect(Collectors.toSet());
        
        int totalPatients = uniquePatients.size();
        
        // Calculate new patients this month
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        
        long newPatientsThisMonth = allAppointments.stream()
            .filter(apt -> {
                Date aptDate = apt.getCreatedAt();
                if (aptDate == null) return false;
                LocalDate aptLocalDate = aptDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return aptLocalDate.isAfter(firstDayOfMonth.minusDays(1)) || aptLocalDate.isEqual(firstDayOfMonth);
            })
            .map(apt -> apt.getPatient().getId())
            .distinct()
            .count();
        
        analytics.put("totalPatients", totalPatients);
        analytics.put("newPatientsThisMonth", (int)newPatientsThisMonth);
        
        // 5. ENGAGEMENT OVER TIME (last 6 months for posts)
        List<Map<String, Object>> monthlyPostEngagement = getMonthlyPostEngagement(postEngagementData, 6);
        analytics.put("monthlyPostEngagement", monthlyPostEngagement);
        
        // 6. CALCULATE KEY METRICS
        // Conversion rate: appointments / profile views (if applicable)
        double conversionRate = profileViews > 0 
            ? ((double)totalAppointments / profileViews) * 100 
            : 0;
        
        // Completion rate
        double completionRate = totalAppointments > 0 
            ? ((double)completedCount / totalAppointments) * 100 
            : 0;
        
        analytics.put("conversionRate", Math.round(conversionRate * 10.0) / 10.0);
        analytics.put("completionRate", Math.round(completionRate * 10.0) / 10.0);
        
        return analytics;
    }
    
    /**
     * Get monthly appointment trends for the last N months
     */
    private List<Map<String, Object>> getMonthlyAppointmentTrends(List<AppointmentEntity> appointments, int months) {
        Map<String, Integer> monthlyCounts = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        
        // Initialize last N months with 0
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthKey = monthDate.getYear() + "-" + String.format("%02d", monthDate.getMonthValue());
            monthlyCounts.put(monthKey, 0);
        }
        
        // Count appointments per month
        for (AppointmentEntity apt : appointments) {
            Date aptDate = apt.getCreatedAt();
            if (aptDate == null) continue;
            
            LocalDate aptLocalDate = aptDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String monthKey = aptLocalDate.getYear() + "-" + String.format("%02d", aptLocalDate.getMonthValue());
            
            if (monthlyCounts.containsKey(monthKey)) {
                monthlyCounts.put(monthKey, monthlyCounts.get(monthKey) + 1);
            }
        }
        
        // Convert to list format
        List<Map<String, Object>> trends = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : monthlyCounts.entrySet()) {
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", entry.getKey());
            monthData.put("count", entry.getValue());
            trends.add(monthData);
        }
        
        return trends;
    }
    
    /**
     * Get monthly post engagement trends for the last N months
     */
    private List<Map<String, Object>> getMonthlyPostEngagement(List<Map<String, Object>> postEngagementData, int months) {
        Map<String, Map<String, Long>> monthlyEngagement = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        
        // Initialize last N months with 0
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthKey = monthDate.getYear() + "-" + String.format("%02d", monthDate.getMonthValue());
            Map<String, Long> engagement = new HashMap<>();
            engagement.put("likes", 0L);
            engagement.put("comments", 0L);
            engagement.put("total", 0L);
            monthlyEngagement.put(monthKey, engagement);
        }
        
        // Aggregate engagement per month
        for (Map<String, Object> postData : postEngagementData) {
            Date createdAt = (Date) postData.get("createdAt");
            if (createdAt == null) continue;
            
            LocalDate postDate = createdAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String monthKey = postDate.getYear() + "-" + String.format("%02d", postDate.getMonthValue());
            
            if (monthlyEngagement.containsKey(monthKey)) {
                Map<String, Long> engagement = monthlyEngagement.get(monthKey);
                engagement.put("likes", engagement.get("likes") + ((Number) postData.get("likes")).longValue());
                engagement.put("comments", engagement.get("comments") + ((Number) postData.get("comments")).longValue());
                engagement.put("total", engagement.get("total") + ((Number) postData.get("engagement")).longValue());
            }
        }
        
        // Convert to list format
        List<Map<String, Object>> trends = new ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> entry : monthlyEngagement.entrySet()) {
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", entry.getKey());
            monthData.put("likes", entry.getValue().get("likes"));
            monthData.put("comments", entry.getValue().get("comments"));
            monthData.put("total", entry.getValue().get("total"));
            trends.add(monthData);
        }
        
        return trends;
    }
}

