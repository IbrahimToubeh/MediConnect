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
import com.MediConnect.socialmedia.repository.MedicalPostRepository;
import com.MediConnect.socialmedia.repository.MedicalPostLikeRepository;
import com.MediConnect.socialmedia.service.NotificationService;
import com.MediConnect.socialmedia.service.post.MedicalPostService;
import com.MediConnect.socialmedia.service.post.mapper.PostMapStructRelated;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MedicalPostServiceImpl implements MedicalPostService {
    private final MedicalPostRepository medicalPostRepository;
    private final PostMapStructRelated postMapStructRelated;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final PatientRepo patientRepo;
    private final MedicalPostLikeRepository medicalPostLikeRepository;
    private final UserRepo userRepo;
    private final NotificationService notificationService;

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

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllPostsWithDetails(Long userId) {
        System.out.println("=== FETCHING POSTS ===");
        System.out.println("User ID: " + userId);
        
        // Fetch posts with fresh data from database
        List<MedicalPost> posts = medicalPostRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> postsWithDetails = new ArrayList<>();

        System.out.println("Found " + posts.size() + " posts");

        for (MedicalPost post : posts) {
            Map<String, Object> postDetails = new HashMap<>();
            postDetails.put("id", post.getId());
            postDetails.put("content", post.getContent());
            postDetails.put("mediaUrl", post.getMediaUrl());
            postDetails.put("createdAt", post.getCreatedAt());
            
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
            }
            
            // Count likes using direct database query (bypasses entity cache)
            long likeCount = medicalPostLikeRepository.countByPostId(post.getId());
            postDetails.put("likes", (int) likeCount);
            System.out.println("Post ID " + post.getId() + " has " + likeCount + " likes (direct DB count query)");
            
            // Check if current user liked this post using direct query
            if (userId != null) {
                List<MedicalPostLike> userLikes = medicalPostLikeRepository.findByPostIdAndLikeGiverId(post.getId(), userId);
                boolean isLiked = !userLikes.isEmpty();
                postDetails.put("isLiked", isLiked);
                System.out.println("User " + userId + " liked post " + post.getId() + ": " + isLiked + " (found " + userLikes.size() + " likes)");
            } else {
                postDetails.put("isLiked", false);
            }
            
            // Count comments
            int commentCount = post.getComments() != null ? post.getComments().size() : 0;
            postDetails.put("comments", commentCount);
            
            postsWithDetails.add(postDetails);
        }

        System.out.println("=== END FETCHING POSTS ===");
        return postsWithDetails;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPostsByDoctor(Long doctorId, Long userId) {
        System.out.println("=== FETCHING POSTS BY DOCTOR ===");
        System.out.println("Doctor ID: " + doctorId);
        System.out.println("User ID: " + userId);
        
        // Fetch posts by specific doctor
        List<MedicalPost> posts = medicalPostRepository.findByPostProviderIdOrderByCreatedAtDesc(doctorId);
        List<Map<String, Object>> postsWithDetails = new ArrayList<>();

        System.out.println("Found " + posts.size() + " posts for doctor " + doctorId);

        for (MedicalPost post : posts) {
            Map<String, Object> postDetails = new HashMap<>();
            postDetails.put("id", post.getId());
            postDetails.put("content", post.getContent());
            postDetails.put("mediaUrl", post.getMediaUrl());
            postDetails.put("createdAt", post.getCreatedAt());
            
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
            }
            
            // Count likes using direct database query (bypasses entity cache)
            long likeCount = medicalPostLikeRepository.countByPostId(post.getId());
            postDetails.put("likes", (int) likeCount);
            System.out.println("Post ID " + post.getId() + " has " + likeCount + " likes (direct DB count query)");
            
            // Check if current user liked this post using direct query
            if (userId != null) {
                List<MedicalPostLike> userLikes = medicalPostLikeRepository.findByPostIdAndLikeGiverId(post.getId(), userId);
                boolean isLiked = !userLikes.isEmpty();
                postDetails.put("isLiked", isLiked);
                System.out.println("User " + userId + " liked post " + post.getId() + ": " + isLiked + " (found " + userLikes.size() + " likes)");
            } else {
                postDetails.put("isLiked", false);
            }
            
            // Count comments
            int commentCount = post.getComments() != null ? post.getComments().size() : 0;
            postDetails.put("comments", commentCount);
            
            postsWithDetails.add(postDetails);
        }

        System.out.println("=== END FETCHING POSTS BY DOCTOR ===");
        return postsWithDetails;
    }

    @Override
    @Transactional
    //todo: change this to cache system using redis
    public boolean likePost(Long postId, Long userId) {
        System.out.println("\n=== LIKE/UNLIKE POST REQUEST ===");
        System.out.println("Post ID: " + postId);
        System.out.println("User ID: " + userId);
        
        // Check current like count
        long currentLikeCount = medicalPostLikeRepository.countByPostId(postId);
        System.out.println("Current total likes for post: " + currentLikeCount);
        
        // Check if user already liked using direct query
        List<MedicalPostLike> existingLikes = medicalPostLikeRepository.findByPostIdAndLikeGiverId(postId, userId);
        
        System.out.println("Found " + existingLikes.size() + " existing likes from user " + userId);
        
        if (!existingLikes.isEmpty()) {
            // User already liked - UNLIKE the post
            System.out.println("→ UNLIKING post...");
            
            // Use direct delete query for efficiency
            int deletedCount = medicalPostLikeRepository.deleteByPostIdAndLikeGiverId(postId, userId);
            System.out.println("Deleted " + deletedCount + " like(s) using direct query");
            
            // Verify deletion
            long newLikeCount = medicalPostLikeRepository.countByPostId(postId);
            System.out.println("New total likes for post: " + newLikeCount + " (decreased from " + currentLikeCount + ")");
            System.out.println("✓ Successfully UNLIKED post");
            System.out.println("=== END LIKE REQUEST ===\n");
            return false; // Unliked
        } else {
            // User hasn't liked yet - LIKE the post
            System.out.println("→ LIKING post...");
            
            Optional<MedicalPost> postOpt = medicalPostRepository.findById(postId);
            if (postOpt.isPresent()) {
                MedicalPost post = postOpt.get();
                MedicalPostLike like = new MedicalPostLike();
                like.setPost(post);
                like.setLikeGiverId(userId);
                like.setCreatedAt(new Date());
                
                MedicalPostLike savedLike = medicalPostLikeRepository.save(like);
                medicalPostLikeRepository.flush();
                
                // Create notification for post owner
                try {
                    Users actor = userRepo.findById(userId).orElse(null);
                    if (actor != null) {
                        notificationService.createPostLikeNotification(actor, post);
                        System.out.println("✓ Created like notification");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to create like notification: " + e.getMessage());
                }
                
                // Verify addition
                long newLikeCount = medicalPostLikeRepository.countByPostId(postId);
                System.out.println("Like saved with ID: " + savedLike.getId());
                System.out.println("New total likes for post: " + newLikeCount + " (increased from " + currentLikeCount + ")");
                System.out.println("✓ Successfully LIKED post");
                System.out.println("=== END LIKE REQUEST ===\n");
                return true; // Liked
            } else {
                System.out.println("✗ Post not found!");
                System.out.println("=== END LIKE REQUEST ===\n");
                return false;
            }
        }
    }

    @Override
    public void deletePost(Long postId, Long userId) {
        MedicalPost post = medicalPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));
            // Check if the user is the owner of the post
            if (post.getPostProvider() != null && Objects.equals(post.getPostProvider().getId(), userId)) {
                medicalPostRepository.delete(post);
            } else {
                throw new RuntimeException("You can only delete your own posts");
            }
    }

    @Override
    public List<Map<String, Object>> getPostLikers(Long postId) {
        System.out.println("=== GET POST LIKERS ===");
        System.out.println("Post ID: " + postId);
        
        // Get all likes for this post
        List<MedicalPostLike> likes = medicalPostLikeRepository.findByPostId(postId);
        List<Map<String, Object>> likersWithDetails = new ArrayList<>();
        
        System.out.println("Found " + likes.size() + " likers");
        
        for (MedicalPostLike like : likes) {
            Map<String, Object> likerDetails = new HashMap<>();
            likerDetails.put("likeId", like.getId());
            likerDetails.put("userId", like.getLikeGiverId());
            likerDetails.put("likedAt", like.getCreatedAt());
            
            // Get user details - check both healthcare providers and patients
            if (like.getLikeGiverId() != null) {
                // Try healthcare provider first
                Optional<HealthcareProvider> providerOpt = healthcareProviderRepo.findById(like.getLikeGiverId());
                if (providerOpt.isPresent()) {
                    HealthcareProvider provider = providerOpt.get();
                    likerDetails.put("name", "Dr. " + provider.getFirstName() + " " + provider.getLastName());
                    String specialty = provider.getSpecializations() != null && !provider.getSpecializations().isEmpty() 
                        ? provider.getSpecializations().get(0).toString() 
                        : "General Practice";
                    likerDetails.put("specialty", specialty);
                    likerDetails.put("userType", "doctor");
                } else {
                    // Try patient
                    Optional<Patient> patientOpt = patientRepo.findById(like.getLikeGiverId());
                    if (patientOpt.isPresent()) {
                        Patient patient = patientOpt.get();
                        likerDetails.put("name", patient.getFirstName() + " " + patient.getLastName());
                        likerDetails.put("specialty", "Patient");
                        likerDetails.put("userType", "patient");
                    } else {
                        // Fallback
                        likerDetails.put("name", "Unknown User");
                        likerDetails.put("specialty", "User");
                        likerDetails.put("userType", "unknown");
                    }
                }
            }
            
            likersWithDetails.add(likerDetails);
        }
        
        System.out.println("Returning " + likersWithDetails.size() + " likers with details");
        return likersWithDetails;
    }
}
