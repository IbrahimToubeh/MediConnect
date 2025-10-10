package com.MediConnect.socialmedia.service.comment.impl;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.socialmedia.dto.CreateCommentRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.entity.MedicalPostComment;
import com.MediConnect.socialmedia.repository.MedicalPostCommentRepository;
import com.MediConnect.socialmedia.repository.MedicalPostRepository;
import com.MediConnect.socialmedia.service.comment.MedicalPostCommentService;
import com.MediConnect.socialmedia.service.comment.mapper.CommentMapStructRelated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MedicalPostCommentServiceImpl implements MedicalPostCommentService {
    private final MedicalPostCommentRepository medicalPostCommentRepository;
    private final CommentMapStructRelated commentMapStructRelated;
    private final MedicalPostRepository postRepository;
    private final HealthcareProviderRepo healthcareProviderRepo;

    @Override
    public void createMedicalPostComment(CreateCommentRequestDTO commentRequestDTO) {
        MedicalPostComment medicalPostComment = commentMapStructRelated.commentRequestDTOToMedicalPostComment(commentRequestDTO);
        MedicalPost medicalPost = postRepository.findById(commentRequestDTO.getPostId()).get();
        medicalPostComment.setPost(medicalPost);
        medicalPostCommentRepository.save(medicalPostComment);
    }

    @Override
    public List<Map<String, Object>> getCommentsByPostId(Long postId) {
        List<MedicalPostComment> comments = medicalPostCommentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        List<Map<String, Object>> commentsWithDetails = new ArrayList<>();

        for (MedicalPostComment comment : comments) {
            Map<String, Object> commentDetails = new HashMap<>();
            commentDetails.put("id", comment.getId());
            commentDetails.put("content", comment.getContent());
            commentDetails.put("createdAt", comment.getCreatedAt());
            commentDetails.put("commenterId", comment.getCommenterId());
            
            // Get commenter details if it's a healthcare provider
            if (comment.getCommenterId() != null) {
                Optional<HealthcareProvider> providerOpt = healthcareProviderRepo.findById(comment.getCommenterId());
                if (providerOpt.isPresent()) {
                    HealthcareProvider provider = providerOpt.get();
                    commentDetails.put("commenterName", provider.getFirstName() + " " + provider.getLastName());
                    // Get first specialization or default
                    String specialty = provider.getSpecializations() != null && !provider.getSpecializations().isEmpty() 
                        ? provider.getSpecializations().get(0).toString() 
                        : "General Practice";
                    commentDetails.put("commenterSpecialty", specialty);
                }
            }
            
            commentsWithDetails.add(commentDetails);
        }

        return commentsWithDetails;
    }
}
