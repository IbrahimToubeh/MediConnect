package com.MediConnect.socialmedia.service.post.impl;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.socialmedia.dto.CreatePostRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.entity.MedicalPostLike;
import com.MediConnect.socialmedia.repository.MedicalPostRepository;
import com.MediConnect.socialmedia.repository.MedicalPostLikeRepository;
import com.MediConnect.socialmedia.service.post.MedicalPostService;
import com.MediConnect.socialmedia.service.post.mapper.PostMapStructRelated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MedicalPostServiceImpl implements MedicalPostService {
    private final MedicalPostRepository medicalPostRepository;
    private final PostMapStructRelated postMapStructRelated;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final MedicalPostLikeRepository medicalPostLikeRepository;

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
    public List<Map<String, Object>> getAllPostsWithDetails() {
        return getAllPostsWithDetails(null);
    }

    @Override
    public List<Map<String, Object>> getAllPostsWithDetails(Long userId) {
        List<MedicalPost> posts = medicalPostRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> postsWithDetails = new ArrayList<>();

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
            
            // Count likes
            int likeCount = post.getLikes() != null ? post.getLikes().size() : 0;
            postDetails.put("likes", likeCount);
            
            // Check if current user liked this post
            if (userId != null && post.getLikes() != null) {
                boolean isLiked = post.getLikes().stream()
                    .anyMatch(like -> Objects.equals(like.getLikeGiverId(), userId));
                postDetails.put("isLiked", isLiked);
            } else {
                postDetails.put("isLiked", false);
            }
            
            // Count comments
            int commentCount = post.getComments() != null ? post.getComments().size() : 0;
            postDetails.put("comments", commentCount);
            
            postsWithDetails.add(postDetails);
        }

        return postsWithDetails;
    }

    @Override
    public boolean likePost(Long postId, Long userId) {
        Optional<MedicalPost> postOpt = medicalPostRepository.findById(postId);
        if (postOpt.isPresent()) {
            MedicalPost post = postOpt.get();
            
            // Check if user already liked this post
            Optional<MedicalPostLike> existingLike = post.getLikes().stream()
                .filter(like -> Objects.equals(like.getLikeGiverId(), userId))
                .findFirst();
            
            if (existingLike.isPresent()) {
                // User already liked, so remove the like (unlike)
                medicalPostLikeRepository.delete(existingLike.get());
                return false; // Unliked
            } else {
                // User hasn't liked yet, so add a like
                MedicalPostLike like = new MedicalPostLike();
                like.setPost(post);
                like.setLikeGiverId(userId);
                like.setCreatedAt(new Date());
                medicalPostLikeRepository.save(like);
                return true; // Liked
            }
        }
        return false;
    }

    @Override
    public void deletePost(Long postId, Long userId) {
        Optional<MedicalPost> postOpt = medicalPostRepository.findById(postId);
        if (postOpt.isPresent()) {
            MedicalPost post = postOpt.get();
            
            // Check if the user is the owner of the post
            if (post.getPostProvider() != null && Objects.equals(post.getPostProvider().getId(), userId)) {
                medicalPostRepository.delete(post);
            } else {
                throw new RuntimeException("You can only delete your own posts");
            }
        } else {
            throw new RuntimeException("Post not found");
        }
    }
}
