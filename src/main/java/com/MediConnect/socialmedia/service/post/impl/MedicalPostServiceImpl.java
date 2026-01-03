package com.MediConnect.socialmedia.service.post.impl;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.Repos.UserRepo;
import com.MediConnect.socialmedia.dto.CreatePostRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.entity.MedicalPostLike;
import com.MediConnect.socialmedia.entity.MedicalPostReport;
import com.MediConnect.socialmedia.entity.enums.PostReporterType;
import com.MediConnect.socialmedia.repository.MedicalPostRepository;
import com.MediConnect.socialmedia.repository.MedicalPostLikeRepository;
import com.MediConnect.socialmedia.repository.MedicalPostCommentRepository;
import com.MediConnect.socialmedia.repository.MedicalPostReportRepository;
import com.MediConnect.socialmedia.repository.NotificationRepository;
import com.MediConnect.socialmedia.service.post.dto.AdminPostFilter;
import com.MediConnect.socialmedia.service.NotificationService;
import com.MediConnect.socialmedia.service.post.MedicalPostService;
import com.MediConnect.socialmedia.service.post.mapper.PostMapStructRelated;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service implementation for managing medical posts, likes, and admin operations.
 * Handles CRUD operations, user interactions, and administrative post management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalPostServiceImpl implements MedicalPostService {
    private final MedicalPostRepository medicalPostRepository;
    private final PostMapStructRelated postMapStructRelated;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final PatientRepo patientRepo;
    private final MedicalPostLikeRepository medicalPostLikeRepository;
    private final MedicalPostCommentRepository medicalPostCommentRepository;
    private final UserRepo userRepo;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final MedicalPostReportRepository medicalPostReportRepository;

    @Override
    public void saveMedicalPost(CreatePostRequestDTO requestDTO) {
        MedicalPost medicalPost = postMapStructRelated.createPostRequestDTOToMedicalPost(requestDTO);
        HealthcareProvider healthcareProvider = healthcareProviderRepo.findById(requestDTO.getProviderId()).get();
        medicalPost.setPostProvider(healthcareProvider);
        medicalPostRepository.save(medicalPost);
    }

    @Override
    public MedicalPost createPost(CreatePostRequestDTO requestDTO) {
        MedicalPost medicalPost = postMapStructRelated.createPostRequestDTOToMedicalPost(requestDTO);
        HealthcareProvider healthcareProvider = healthcareProviderRepo.findById(requestDTO.getProviderId()).get();
        medicalPost.setPostProvider(healthcareProvider);
        return medicalPostRepository.save(medicalPost);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllPostsWithDetails() {
        return getAllPostsWithDetails(null);
    }

    /**
     * Retrieves all posts with full details including likes, comments, and user-specific information.
     * Uses batch fetching to optimize performance and avoid N+1 query problems.
     * 
     * @param userId The ID of the current user (can be null for anonymous users)
     * @return List of post detail maps
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllPostsWithDetails(Long userId) {
        log.debug("Fetching all posts with details for user: {}", userId);
        
        // Fetch all posts (1 query)
        List<MedicalPost> posts = medicalPostRepository.findAllByOrderByCreatedAtDesc();
        
        if (posts.isEmpty()) {
            log.debug("No posts found");
            return new ArrayList<>();
        }
        
        // Batch fetch all related data to avoid N+1 queries
        BatchPostData batchData = batchFetchPostData(posts, userId);
        
        // Build DTOs using pre-fetched batch data
        return posts.stream()
            .map(post -> buildPostDTO(post, userId, batchData))
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all posts with pagination support.
     * If page and size are null, returns all posts (backward compatible behavior).
     * Uses batch fetching to optimize performance and avoid N+1 query problems.
     * 
     * @param userId The ID of the current user (can be null for anonymous users)
     * @param page Page number (0-indexed, null to return all)
     * @param size Page size (null to return all)
     * @return Map containing "data" (list of posts), "totalElements", "totalPages", "currentPage", "pageSize"
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllPostsWithDetails(Long userId, Integer page, Integer size) {
        log.debug("Fetching all posts with details for user: {}, page: {}, size: {}", userId, page, size);
        
        List<MedicalPost> posts;
        long totalElements;
        
        if (page != null && size != null) {
            // Paginated query
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<MedicalPost> postPage = medicalPostRepository.findAll(pageable);
            posts = postPage.getContent();
            totalElements = postPage.getTotalElements();
            log.debug("Fetched {} posts (page {} of {}, total: {})", posts.size(), page, postPage.getTotalPages(), totalElements);
        } else {
            // Non-paginated query (backward compatible)
            posts = medicalPostRepository.findAllByOrderByCreatedAtDesc();
            totalElements = posts.size();
            log.debug("Fetched all {} posts (no pagination)", totalElements);
        }
        
        if (posts.isEmpty()) {
            log.debug("No posts found");
            return buildPaginationResponse(new ArrayList<>(), totalElements, page, size);
        }
        
        // Batch fetch all related data to avoid N+1 queries
        BatchPostData batchData = batchFetchPostData(posts, userId);
        
        // Build DTOs using pre-fetched batch data
        List<Map<String, Object>> postDTOs = posts.stream()
            .map(post -> buildPostDTO(post, userId, batchData))
            .collect(Collectors.toList());
        
        return buildPaginationResponse(postDTOs, totalElements, page, size);
    }
    
    /**
     * Retrieves all posts by a specific doctor with full details.
     * Uses batch fetching to optimize performance and avoid N+1 query problems.
     * 
     * @param doctorId The ID of the doctor
     * @param userId The ID of the current user (can be null for anonymous users)
     * @return List of post detail maps
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPostsByDoctor(Long doctorId, Long userId) {
        log.debug("Fetching posts by doctor {} for user: {}", doctorId, userId);
        
        // Fetch posts by specific doctor (1 query)
        List<MedicalPost> posts = medicalPostRepository.findByPostProviderIdOrderByCreatedAtDesc(doctorId);
        
        if (posts.isEmpty()) {
            log.debug("No posts found for doctor {}", doctorId);
            return new ArrayList<>();
        }
        
        // Batch fetch all related data to avoid N+1 queries
        BatchPostData batchData = batchFetchPostData(posts, userId);
        
        // Build DTOs using pre-fetched batch data
        return posts.stream()
            .map(post -> buildPostDTO(post, userId, batchData))
            .collect(Collectors.toList());
    }
    
    /**
     * Retrieves posts by doctor with pagination support.
     * If page and size are null, returns all posts (backward compatible behavior).
     * Uses batch fetching to optimize performance and avoid N+1 query problems.
     * 
     * @param doctorId The ID of the doctor
     * @param userId The ID of the current user (can be null for anonymous users)
     * @param page Page number (0-indexed, null to return all)
     * @param size Page size (null to return all)
     * @return Map containing "data" (list of posts), "totalElements", "totalPages", "currentPage", "pageSize"
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPostsByDoctor(Long doctorId, Long userId, Integer page, Integer size) {
        log.debug("Fetching posts by doctor {} for user: {}, page: {}, size: {}", doctorId, userId, page, size);
        
        List<MedicalPost> posts;
        long totalElements;
        
        if (page != null && size != null) {
            // Paginated query - need to count total first, then fetch page
            long totalCount = medicalPostRepository.findByPostProviderIdOrderByCreatedAtDesc(doctorId).size();
            totalElements = totalCount;
            
            // Manual pagination for custom query
            List<MedicalPost> allPosts = medicalPostRepository.findByPostProviderIdOrderByCreatedAtDesc(doctorId);
            int start = page * size;
            int end = Math.min(start + size, allPosts.size());
            posts = start < allPosts.size() ? allPosts.subList(start, end) : new ArrayList<>();
            
            log.debug("Fetched {} posts (page {} of {}, total: {})", posts.size(), page, 
                    (int) Math.ceil((double) totalElements / size), totalElements);
        } else {
            // Non-paginated query (backward compatible)
            posts = medicalPostRepository.findByPostProviderIdOrderByCreatedAtDesc(doctorId);
            totalElements = posts.size();
            log.debug("Fetched all {} posts for doctor {} (no pagination)", totalElements, doctorId);
        }
        
        if (posts.isEmpty()) {
            log.debug("No posts found for doctor {}", doctorId);
            return buildPaginationResponse(new ArrayList<>(), totalElements, page, size);
        }
        
        // Batch fetch all related data to avoid N+1 queries
        BatchPostData batchData = batchFetchPostData(posts, userId);
        
        // Build DTOs using pre-fetched batch data
        List<Map<String, Object>> postDTOs = posts.stream()
            .map(post -> buildPostDTO(post, userId, batchData))
            .collect(Collectors.toList());
        
        return buildPaginationResponse(postDTOs, totalElements, page, size);
    }

    /**
     * Toggles like status for a post by a user.
     * If the user has already liked the post, it will be unliked.
     * If not, a new like will be created and a notification sent to the post owner.
     * 
     * @param postId The ID of the post to like/unlike
     * @param userId The ID of the user performing the action
     * @return true if the post was liked, false if it was unliked or post not found
     */
    @Override
    @Transactional
    //todo: change this to cache system using redis
    public boolean likePost(Long postId, Long userId) {
        log.debug("Processing like/unlike request - Post ID: {}, User ID: {}", postId, userId);
        
        // Check current like count
        long currentLikeCount = medicalPostLikeRepository.countByPostId(postId);
        log.debug("Current total likes for post {}: {}", postId, currentLikeCount);
        
        // Check if user already liked using direct query
        List<MedicalPostLike> existingLikes = medicalPostLikeRepository.findByPostIdAndLikeGiverId(postId, userId);
        log.debug("Found {} existing likes from user {} for post {}", existingLikes.size(), userId, postId);
        
        if (!existingLikes.isEmpty()) {
            // User already liked - UNLIKE the post
            log.debug("User {} already liked post {}, removing like", userId, postId);
            
            // Use direct delete query for efficiency
            int deletedCount = medicalPostLikeRepository.deleteByPostIdAndLikeGiverId(postId, userId);
            log.debug("Deleted {} like(s) using direct query", deletedCount);
            
            // Verify deletion
            long newLikeCount = medicalPostLikeRepository.countByPostId(postId);
            log.info("Post {} unliked by user {}. Like count: {} -> {}", postId, userId, currentLikeCount, newLikeCount);
            return false; // Unliked
        } else {
            // User hasn't liked yet - LIKE the post
            log.debug("User {} has not liked post {} yet, adding like", userId, postId);
            
            Optional<MedicalPost> postOpt = medicalPostRepository.findById(postId);
            if (postOpt.isPresent()) {
                MedicalPost post = postOpt.get();
                MedicalPostLike like = new MedicalPostLike();
                like.setPost(post);
                like.setLikeGiverId(userId);
                like.setCreatedAt(new Date());
                
                // Save the like - no explicit flush() needed
                // Spring's @Transactional will automatically flush at transaction commit
                // The entity is already in the persistence context and available for subsequent operations
                MedicalPostLike savedLike = medicalPostLikeRepository.save(like);
                
                // Create notification for post owner
                try {
                    Users actor = userRepo.findById(userId).orElse(null);
                    if (actor != null) {
                        notificationService.createPostLikeNotification(actor, post);
                        log.debug("Created like notification for post owner");
                    }
                } catch (Exception e) {
                    log.error("Failed to create like notification for post {} by user {}: {}", 
                            postId, userId, e.getMessage(), e);
                }
                
                // Verify addition
                long newLikeCount = medicalPostLikeRepository.countByPostId(postId);
                log.info("Post {} liked by user {}. Like saved with ID: {}. Like count: {} -> {}", 
                        postId, userId, savedLike.getId(), currentLikeCount, newLikeCount);
                return true; // Liked
            } else {
                log.warn("Post {} not found when user {} attempted to like it", postId, userId);
                return false;
            }
        }
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        MedicalPost post = medicalPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));
        
        // Check if the user is the owner of the post
        if (post.getPostProvider() != null && Objects.equals(post.getPostProvider().getId(), userId)) {
            removePostWithDependencies(post);
        } else {
            throw new RuntimeException("You can only delete your own posts");
        }
    }

    /**
     * Retrieves all users who liked a specific post with their details.
     * This is a read-only query operation.
     * 
     * @param postId The ID of the post
     * @return List of maps containing liker details (name, specialty, userType, etc.)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPostLikers(Long postId) {
        // Get all likes for this post
        List<MedicalPostLike> likes = medicalPostLikeRepository.findByPostId(postId);
        
        // Build liker DTOs using helper method
        return likes.stream()
            .map(this::buildLikerDetails)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void flagPost(Long postId, String reason) {
        MedicalPost post = medicalPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));
        post.setAdminFlagged(true);
        post.setAdminFlagReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : null);
        post.setAdminFlaggedAt(new Date());
        medicalPostRepository.save(post);
    }

    @Override
    @Transactional
    public void unflagPost(Long postId) {
        MedicalPost post = medicalPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));
        post.setAdminFlagged(false);
        post.setAdminFlagReason(null);
        post.setAdminFlaggedAt(null);
        medicalPostRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFlaggedPosts() {
        List<MedicalPost> posts = medicalPostRepository.findByAdminFlaggedTrueOrderByAdminFlaggedAtDesc();
        
        // Build admin DTOs using helper method
        return posts.stream()
            .map(post -> buildPostDTOForAdmin(post, medicalPostReportRepository.countByPostId(post.getId())))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void adminDeletePost(Long postId) {
        MedicalPost post = medicalPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));
        removePostWithDependencies(post);
    }

    /**
     * Retrieves all reports for a specific post with reporter details.
     * Uses batch fetching to optimize performance and avoid N+1 query problems.
     * 
     * @param postId The ID of the post to get reports for
     * @return List of report detail maps with reporter information
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAdminPostReports(Long postId) {
        log.debug("Fetching admin post reports for post ID: {}", postId);
        
        MedicalPost post = medicalPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));

        List<MedicalPostReport> reports = medicalPostReportRepository.findByPost(post);
        
        if (reports.isEmpty()) {
            log.debug("No reports found for post {}", postId);
            return new ArrayList<>();
        }
        
        // Batch fetch all reporter details to avoid N+1 queries
        Map<Long, HealthcareProvider> doctorMap = batchFetchReporters(reports);
        Map<Long, Patient> patientMap = batchFetchPatientReporters(reports);
        
        // Build report DTOs using pre-fetched reporter data
        List<Map<String, Object>> result = new ArrayList<>();
        for (MedicalPostReport report : reports) {
            Map<String, Object> reportDetails = buildReportDetails(report, doctorMap, patientMap);
            result.add(reportDetails);
        }

        log.debug("Returning {} reports for post {}", result.size(), postId);
        return result;
    }
    
    /**
     * Batch fetches all doctor reporters in a single query.
     * This eliminates N+1 query problem when getting reporter details.
     * 
     * @param reports List of reports to extract doctor IDs from
     * @return Map of reporterId -> HealthcareProvider
     */
    private Map<Long, HealthcareProvider> batchFetchReporters(List<MedicalPostReport> reports) {
        // Extract all doctor reporter IDs
        List<Long> doctorIds = reports.stream()
            .filter(report -> report.getReporterType() == PostReporterType.DOCTOR)
            .map(MedicalPostReport::getReporterId)
            .distinct()
            .collect(Collectors.toList());
        
        if (doctorIds.isEmpty()) {
            return new HashMap<>();
        }
        
        // Batch fetch all doctors in one query
        List<HealthcareProvider> doctors = healthcareProviderRepo.findAllById(doctorIds);
        
        // Build map for quick lookup
        Map<Long, HealthcareProvider> doctorMap = doctors.stream()
            .collect(Collectors.toMap(HealthcareProvider::getId, doctor -> doctor));
        
        log.debug("Batch fetched {} doctor reporters", doctorMap.size());
        return doctorMap;
    }
    
    /**
     * Batch fetches all patient reporters in a single query.
     * This eliminates N+1 query problem when getting reporter details.
     * 
     * @param reports List of reports to extract patient IDs from
     * @return Map of reporterId -> Patient
     */
    private Map<Long, Patient> batchFetchPatientReporters(List<MedicalPostReport> reports) {
        // Extract all patient reporter IDs
        List<Long> patientIds = reports.stream()
            .filter(report -> report.getReporterType() == PostReporterType.PATIENT)
            .map(MedicalPostReport::getReporterId)
            .distinct()
            .collect(Collectors.toList());
        
        if (patientIds.isEmpty()) {
            return new HashMap<>();
        }
        
        // Batch fetch all patients in one query
        List<Patient> patients = patientRepo.findAllById(patientIds);
        
        // Build map for quick lookup
        Map<Long, Patient> patientMap = patients.stream()
            .collect(Collectors.toMap(Patient::getId, patient -> patient));
        
        log.debug("Batch fetched {} patient reporters", patientMap.size());
        return patientMap;
    }
    
    /**
     * Builds a report details map from a MedicalPostReport entity.
     * Uses pre-fetched reporter maps to avoid individual database queries.
     * 
     * @param report The report entity
     * @param doctorMap Pre-fetched map of doctor reporters (reporterId -> HealthcareProvider)
     * @param patientMap Pre-fetched map of patient reporters (reporterId -> Patient)
     * @return Map containing report details with reporter information
     */
    private Map<String, Object> buildReportDetails(MedicalPostReport report, 
                                                   Map<Long, HealthcareProvider> doctorMap,
                                                   Map<Long, Patient> patientMap) {
        Map<String, Object> reportDetails = new HashMap<>();
        reportDetails.put("id", report.getId());
        reportDetails.put("reason", report.getReason());
        reportDetails.put("reporterType", report.getReporterType());
        reportDetails.put("reporterId", report.getReporterId());
        reportDetails.put("otherReason", report.getOtherReason());
        reportDetails.put("details", report.getDetails());
        reportDetails.put("createdAt", report.getCreatedAt());
        reportDetails.put("reviewed", report.isReviewed());

        // Get reporter details from pre-fetched maps (avoids N+1 queries)
        if (report.getReporterType() == PostReporterType.DOCTOR) {
            HealthcareProvider provider = doctorMap.get(report.getReporterId());
            if (provider != null) {
                reportDetails.put("reporterName", "Dr. " + provider.getFirstName() + " " + provider.getLastName());
                reportDetails.put("reporterSpecialty", provider.getSpecializations() != null && !provider.getSpecializations().isEmpty()
                        ? provider.getSpecializations().get(0).toString()
                        : "General Practice");
            }
        } else if (report.getReporterType() == PostReporterType.PATIENT) {
            Patient patient = patientMap.get(report.getReporterId());
            if (patient != null) {
                reportDetails.put("reporterName", patient.getFirstName() + " " + patient.getLastName());
                reportDetails.put("reporterSpecialty", "Patient");
            }
        }

        // Fallback if reporter not found
        if (!reportDetails.containsKey("reporterName")) {
            reportDetails.put("reporterName", "Unknown");
            reportDetails.put("reporterSpecialty", report.getReporterType().name().toLowerCase());
        }

        return reportDetails;
    }

    /**
     * Retrieves all posts for admin view with optional filtering.
     * All filtering is done at the database level for optimal performance.
     * Report counts are batch-fetched to avoid N+1 queries.
     * 
     * @param filter Filter criteria (can be null to return all posts)
     * @return List of post detail maps matching the filter criteria
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllPostsForAdmin(AdminPostFilter filter) {
        log.debug("Fetching posts for admin with filter: {}", filter);
        
        // Build dynamic specification based on filter criteria
        Specification<MedicalPost> spec = buildAdminPostSpecification(filter);
        
        // Fetch filtered posts from database (all filtering done at DB level)
        List<MedicalPost> posts = medicalPostRepository.findAll(spec, 
            org.springframework.data.domain.Sort.by("createdAt").descending());
        
        if (posts.isEmpty()) {
            log.debug("No posts found matching filter criteria");
            return new ArrayList<>();
        }
        
        // Batch fetch report counts for all filtered posts (1 query instead of N)
        Map<Long, Long> reportCounts = batchFetchReportCounts(posts);
        
        // Apply reportedOnly filter if specified (must be done after getting report counts)
        final List<MedicalPost> finalPosts;
        if (filter != null && Boolean.TRUE.equals(filter.getReportedOnly())) {
            finalPosts = posts.stream()
                .filter(post -> reportCounts.getOrDefault(post.getId(), 0L) > 0)
                .collect(Collectors.toList());
            log.debug("Applied reportedOnly filter: {} posts remaining out of {}", 
                    finalPosts.size(), posts.size());
        } else {
            finalPosts = posts;
        }
        
        // Build DTOs using helper method
        return finalPosts.stream()
            .map(post -> {
                long reportCount = reportCounts.getOrDefault(post.getId(), 0L);
                return buildPostDTOForAdmin(post, reportCount);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Retrieves all posts for admin view with optional filtering and pagination support.
     * If page and size are null, returns all posts (backward compatible behavior).
     * All filtering is done at the database level for optimal performance.
     * Report counts are batch-fetched to avoid N+1 queries.
     * 
     * @param filter Filter criteria (can be null to return all posts)
     * @param page Page number (0-indexed, null to return all)
     * @param size Page size (null to return all)
     * @return Map containing "data" (list of posts), "totalElements", "totalPages", "currentPage", "pageSize"
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllPostsForAdmin(AdminPostFilter filter, Integer page, Integer size) {
        log.debug("Fetching posts for admin with filter: {}, page: {}, size: {}", filter, page, size);
        
        // Build dynamic specification based on filter criteria
        Specification<MedicalPost> spec = buildAdminPostSpecification(filter);
        
        List<MedicalPost> posts;
        long totalElements;
        
        if (page != null && size != null) {
            // Paginated query
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<MedicalPost> postPage = medicalPostRepository.findAll(spec, pageable);
            posts = postPage.getContent();
            totalElements = postPage.getTotalElements();
            log.debug("Fetched {} posts (page {} of {}, total: {})", posts.size(), page, postPage.getTotalPages(), totalElements);
        } else {
            // Non-paginated query (backward compatible)
            posts = medicalPostRepository.findAll(spec, Sort.by("createdAt").descending());
            totalElements = posts.size();
            log.debug("Fetched all {} posts (no pagination)", totalElements);
        }
        
        if (posts.isEmpty()) {
            log.debug("No posts found matching filter criteria");
            return buildPaginationResponse(new ArrayList<>(), totalElements, page, size);
        }
        
        // Batch fetch report counts for all filtered posts (1 query instead of N)
        Map<Long, Long> reportCounts = batchFetchReportCounts(posts);
        
        // Apply reportedOnly filter if specified (must be done after getting report counts)
        final List<MedicalPost> finalPosts;
        if (filter != null && Boolean.TRUE.equals(filter.getReportedOnly())) {
            finalPosts = posts.stream()
                .filter(post -> reportCounts.getOrDefault(post.getId(), 0L) > 0)
                .collect(Collectors.toList());
            log.debug("Applied reportedOnly filter: {} posts remaining out of {}", 
                    finalPosts.size(), posts.size());
            // Update totalElements to reflect filtered count
            totalElements = finalPosts.size();
        } else {
            finalPosts = posts;
        }
        
        // Build DTOs using helper method
        List<Map<String, Object>> postDTOs = finalPosts.stream()
            .map(post -> {
                long reportCount = reportCounts.getOrDefault(post.getId(), 0L);
                return buildPostDTOForAdmin(post, reportCount);
            })
            .collect(Collectors.toList());
        
        return buildPaginationResponse(postDTOs, totalElements, page, size);
    }
    
    /**
     * Builds a pagination response map with data and pagination metadata.
     * If page and size are null, returns response without pagination metadata (backward compatible).
     * 
     * @param data List of post data
     * @param totalElements Total number of elements
     * @param page Page number (0-indexed, null if not paginated)
     * @param size Page size (null if not paginated)
     * @return Map containing "data" and optional pagination metadata
     */
    private Map<String, Object> buildPaginationResponse(List<Map<String, Object>> data, long totalElements, Integer page, Integer size) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        
        // Add pagination metadata only if pagination is used
        if (page != null && size != null) {
            response.put("totalElements", totalElements);
            response.put("totalPages", (int) Math.ceil((double) totalElements / size));
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("hasNext", (page + 1) * size < totalElements);
            response.put("hasPrevious", page > 0);
        }
        
        return response;
    }
    
    /**
     * Builds a JPA Specification for filtering posts based on AdminPostFilter criteria.
     * All filtering logic is moved to database level for better performance.
     * 
     * @param filter Filter criteria (can be null)
     * @return Specification for querying posts
     */
    private Specification<MedicalPost> buildAdminPostSpecification(AdminPostFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter == null) {
                // No filter - return all posts
                return criteriaBuilder.conjunction();
            }
            
            // Filter by start date (createdAt >= startDate)
            if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), filter.getStartDate()));
            }
            
            // Filter by end date (createdAt <= endDate)
            if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), filter.getEndDate()));
            }
            
            // Filter by flagged status (adminFlagged = true)
            if (Boolean.TRUE.equals(filter.getFlaggedOnly())) {
                predicates.add(criteriaBuilder.isTrue(root.get("adminFlagged")));
            }
            
            // Filter by doctor name (case-insensitive contains search)
            // Matches original logic: checks if filter name is contained in "firstName lastName"
            if (StringUtils.hasText(filter.getDoctorName())) {
                Join<MedicalPost, HealthcareProvider> providerJoin = 
                    root.join("postProvider", JoinType.INNER);
                
                String normalizedDoctorName = filter.getDoctorName().trim().toLowerCase(Locale.ROOT);
                
                // Concatenate firstName + " " + lastName, convert to lowercase, then check contains
                // This matches the original Java-side filtering logic exactly
                Predicate fullNameMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(
                        criteriaBuilder.concat(
                            criteriaBuilder.concat(providerJoin.get("firstName"), " "),
                            providerJoin.get("lastName")
                        )
                    ),
                    "%" + normalizedDoctorName + "%"
                );
                
                predicates.add(fullNameMatch);
            }
            
            // Note: reportedOnly filter is applied after fetching report counts
            // because it requires counting reports, which is more efficient to do
            // in a separate batch query than using a subquery in the main query
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Batch fetches report counts for multiple posts in a single query.
     * This eliminates N+1 query problem when getting report counts.
     * 
     * @param posts List of posts to get report counts for
     * @return Map of postId -> report count
     */
    private Map<Long, Long> batchFetchReportCounts(List<MedicalPost> posts) {
        if (posts.isEmpty()) {
            return new HashMap<>();
        }
        
        List<Long> postIds = posts.stream()
            .map(MedicalPost::getId)
            .collect(Collectors.toList());
        
        // Batch fetch report counts for all posts (1 query instead of N)
        List<Object[]> reportCountResults = medicalPostReportRepository.countReportsByPostIds(postIds);
        Map<Long, Long> reportCounts = new HashMap<>();
        for (Object[] result : reportCountResults) {
            Long postId = (Long) result[0];
            Long count = (Long) result[1];
            reportCounts.put(postId, count);
        }
        
        log.debug("Batch fetched report counts for {} posts: {} posts have reports", 
                postIds.size(), reportCounts.size());
        return reportCounts;
    }

    /**
     * Data class to hold batch-fetched post data to avoid N+1 queries.
     * Contains pre-computed maps for like counts, user likes, and comment counts.
     */
    private static class BatchPostData {
        final Map<Long, Long> likeCounts;      // postId -> like count
        final Set<Long> userLikedPostIds;      // Set of post IDs liked by the user
        final Map<Long, Long> commentCounts;   // postId -> comment count
        
        BatchPostData(Map<Long, Long> likeCounts, Set<Long> userLikedPostIds, Map<Long, Long> commentCounts) {
            this.likeCounts = likeCounts;
            this.userLikedPostIds = userLikedPostIds;
            this.commentCounts = commentCounts;
        }
    }
    
    /**
     * Batch fetches all related data for a list of posts in minimal queries.
     * This method eliminates N+1 query problems by fetching all likes, user likes,
     * and comment counts in just 2-3 queries instead of N queries per post.
     * 
     * @param posts List of posts to fetch data for
     * @param userId The ID of the current user (can be null for anonymous users)
     * @return BatchPostData containing pre-computed maps for all post data
     */
    private BatchPostData batchFetchPostData(List<MedicalPost> posts, Long userId) {
        // Extract post IDs
        List<Long> postIds = posts.stream()
            .map(MedicalPost::getId)
            .collect(Collectors.toList());
        
        if (postIds.isEmpty()) {
            return new BatchPostData(new HashMap<>(), new HashSet<>(), new HashMap<>());
        }
        
        // Batch fetch like counts for all posts (1 query)
        List<Object[]> likeCountResults = medicalPostLikeRepository.countLikesByPostIds(postIds);
        Map<Long, Long> likeCounts = new HashMap<>();
        for (Object[] result : likeCountResults) {
            Long postId = (Long) result[0];
            Long count = (Long) result[1];
            likeCounts.put(postId, count);
        }
        
        // Batch fetch user-specific likes (1 query, only if userId is provided)
        Set<Long> userLikedPostIds = new HashSet<>();
        if (userId != null) {
            List<Long> likedPostIds = medicalPostLikeRepository.findPostIdsLikedByUser(postIds, userId);
            userLikedPostIds.addAll(likedPostIds);
        }
        
        // Batch fetch comment counts for all posts (1 query)
        List<Object[]> commentCountResults = medicalPostCommentRepository.countCommentsByPostIds(postIds);
        Map<Long, Long> commentCounts = new HashMap<>();
        for (Object[] result : commentCountResults) {
            Long postId = (Long) result[0];
            Long count = (Long) result[1];
            commentCounts.put(postId, count);
        }
        
        log.debug("Batch fetched data for {} posts: {} like counts, {} user likes, {} comment counts",
                postIds.size(), likeCounts.size(), userLikedPostIds.size(), commentCounts.size());
        
        return new BatchPostData(likeCounts, userLikedPostIds, commentCounts);
    }
    
    /**
     * Builds a post DTO map from a MedicalPost entity.
     * Includes user-specific information like whether the current user liked the post.
     * Uses pre-fetched batch data to avoid individual database queries.
     * 
     * @param post The post entity
     * @param userId The ID of the current user (can be null for anonymous users)
     * @param batchData Pre-fetched batch data containing like counts, user likes, and comment counts
     * @return Map containing post details with user-specific information
     */
    private Map<String, Object> buildPostDTO(MedicalPost post, Long userId, BatchPostData batchData) {
        Map<String, Object> postDetails = new HashMap<>();
        
        // Basic post information
        postDetails.put("id", post.getId());
        postDetails.put("content", post.getContent());
        postDetails.put("mediaUrl", post.getMediaUrl()); // For backward compatibility
        postDetails.put("mediaUrls", post.getMediaUrls()); // JSON array of media URLs
        postDetails.put("createdAt", post.getCreatedAt());
        postDetails.put("adminFlagged", Boolean.TRUE.equals(post.getAdminFlagged()));
        postDetails.put("adminFlagReason", post.getAdminFlagReason());
        postDetails.put("adminFlaggedAt", post.getAdminFlaggedAt());
        
        // Doctor details
        HealthcareProvider provider = post.getPostProvider();
        if (provider != null) {
            postDetails.put("doctorName", provider.getFirstName() + " " + provider.getLastName());
            // Get first specialization or default
            String specialty = provider.getSpecializations() != null && !provider.getSpecializations().isEmpty() 
                ? provider.getSpecializations().get(0).toString() 
                : "General Practice";
            postDetails.put("doctorSpecialty", specialty);
            postDetails.put("doctorId", provider.getId());
            // Include doctor profile picture
            postDetails.put("doctorProfilePicture", provider.getProfilePicture());
        }
        
        // Get like count from batch data (avoids N+1 query)
        long likeCount = batchData.likeCounts.getOrDefault(post.getId(), 0L);
        postDetails.put("likes", (int) likeCount);
        
        // Check if current user liked this post from batch data (avoids N+1 query)
        boolean isLiked = userId != null && batchData.userLikedPostIds.contains(post.getId());
        postDetails.put("isLiked", isLiked);
        
        // Get comment count from batch data (avoids N+1 query and lazy loading)
        long commentCount = batchData.commentCounts.getOrDefault(post.getId(), 0L);
        postDetails.put("comments", (int) commentCount);
        
        return postDetails;
    }
    
    /**
     * Builds a post DTO map for admin views.
     * Includes additional admin-specific information like report count.
     * 
     * @param post The post entity
     * @param reportCount The number of reports for this post
     * @return Map containing post details for admin view
     */
    private Map<String, Object> buildPostDTOForAdmin(MedicalPost post, long reportCount) {
        Map<String, Object> postDetails = new HashMap<>();
        
        // Basic post information
        postDetails.put("id", post.getId());
        postDetails.put("content", post.getContent());
        postDetails.put("mediaUrl", post.getMediaUrl());
        postDetails.put("mediaUrls", post.getMediaUrls());
        postDetails.put("createdAt", post.getCreatedAt());
        postDetails.put("adminFlagged", Boolean.TRUE.equals(post.getAdminFlagged()));
        postDetails.put("adminFlagReason", post.getAdminFlagReason());
        postDetails.put("adminFlaggedAt", post.getAdminFlaggedAt());
        postDetails.put("reportCount", reportCount);
        
        // Doctor details
        HealthcareProvider provider = post.getPostProvider();
        if (provider != null) {
            postDetails.put("doctorName", provider.getFirstName() + " " + provider.getLastName());
            String specialty = provider.getSpecializations() != null && !provider.getSpecializations().isEmpty()
                    ? provider.getSpecializations().get(0).toString()
                    : "General Practice";
            postDetails.put("doctorSpecialty", specialty);
            postDetails.put("doctorId", provider.getId());
            // Include doctor profile picture
            postDetails.put("doctorProfilePicture", provider.getProfilePicture());
        }
        
        // Count likes and comments
        long likeCount = medicalPostLikeRepository.countByPostId(post.getId());
        postDetails.put("likes", (int) likeCount);
        postDetails.put("comments", post.getComments() != null ? post.getComments().size() : 0);
        
        return postDetails;
    }
    
    /**
     * Builds user details map for a given user ID.
     * Checks both healthcare providers and patients to find the user.
     * 
     * @param userId The ID of the user
     * @return Map containing user details (name, specialty, userType) or null if user not found
     */
    private Map<String, Object> buildUserDetails(Long userId) {
        if (userId == null) {
            return null;
        }
        
        Map<String, Object> userDetails = new HashMap<>();
        
        // Try healthcare provider first
        Optional<HealthcareProvider> providerOpt = healthcareProviderRepo.findById(userId);
        if (providerOpt.isPresent()) {
            HealthcareProvider provider = providerOpt.get();
            userDetails.put("name", "Dr. " + provider.getFirstName() + " " + provider.getLastName());
            String specialty = provider.getSpecializations() != null && !provider.getSpecializations().isEmpty() 
                ? provider.getSpecializations().get(0).toString() 
                : "General Practice";
            userDetails.put("specialty", specialty);
            userDetails.put("userType", "doctor");
            // Include profile picture
            userDetails.put("profilePicture", provider.getProfilePicture());
            return userDetails;
        }
        
        // Try patient
        Optional<Patient> patientOpt = patientRepo.findById(userId);
        if (patientOpt.isPresent()) {
            Patient patient = patientOpt.get();
            userDetails.put("name", patient.getFirstName() + " " + patient.getLastName());
            userDetails.put("specialty", "Patient");
            userDetails.put("userType", "patient");
            // Include profile picture
            userDetails.put("profilePicture", patient.getProfilePicture());
            return userDetails;
        }
        
        // Fallback for unknown users
        userDetails.put("name", "Unknown User");
        userDetails.put("specialty", "User");
        userDetails.put("userType", "unknown");
        userDetails.put("profilePicture", null);
        return userDetails;
    }
    
    /**
     * Builds a liker details map from a like entity.
     * Includes like information and user details.
     * 
     * @param like The like entity
     * @return Map containing liker details (likeId, userId, likedAt, name, specialty, userType)
     */
    private Map<String, Object> buildLikerDetails(MedicalPostLike like) {
        Map<String, Object> likerDetails = new HashMap<>();
        likerDetails.put("likeId", like.getId());
        likerDetails.put("userId", like.getLikeGiverId());
        likerDetails.put("likedAt", like.getCreatedAt());
        
        // Get user details
        Map<String, Object> userDetails = buildUserDetails(like.getLikeGiverId());
        if (userDetails != null) {
            likerDetails.putAll(userDetails);
        }
        
        return likerDetails;
    }

    private void removePostWithDependencies(MedicalPost post) {
        // Delete notifications related to the post
        notificationRepository.deleteByPost(post);

        // Delete notifications related to comments
        if (post.getComments() != null && !post.getComments().isEmpty()) {
            for (com.MediConnect.socialmedia.entity.MedicalPostComment comment : post.getComments()) {
                notificationRepository.deleteByComment(comment);
            }
        }

        // Delete reports related to the post (to avoid foreign key constraint violation)
        List<MedicalPostReport> reports = medicalPostReportRepository.findByPost(post);
        if (reports != null && !reports.isEmpty()) {
            medicalPostReportRepository.deleteAll(reports);
        }

        // Delete the post (cascade will handle ratings, likes, and comments)
        medicalPostRepository.delete(post);
    }
}
