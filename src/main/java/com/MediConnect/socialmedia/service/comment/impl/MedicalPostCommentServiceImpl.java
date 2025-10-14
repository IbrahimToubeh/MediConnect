package com.MediConnect.socialmedia.service.comment.impl;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.Repos.UserRepo;
import com.MediConnect.socialmedia.dto.CreateCommentRequestDTO;
import com.MediConnect.socialmedia.entity.*;
import com.MediConnect.socialmedia.repository.*;
import com.MediConnect.socialmedia.service.NotificationService;
import com.MediConnect.socialmedia.service.comment.MedicalPostCommentService;
import com.MediConnect.socialmedia.service.comment.mapper.CommentMapStructRelated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MedicalPostCommentServiceImpl implements MedicalPostCommentService {
    private final MedicalPostCommentRepository medicalPostCommentRepository;
    private final CommentMapStructRelated commentMapStructRelated;
    private final MedicalPostRepository postRepository;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final PatientRepo patientRepo;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentReplyRepository commentReplyRepository;
    private final CommentReplyLikeRepository commentReplyLikeRepository;
    private final UserRepo userRepo;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void createMedicalPostComment(CreateCommentRequestDTO commentRequestDTO) {
        System.out.println("=== CREATE COMMENT ===");
        System.out.println("Post ID: " + commentRequestDTO.getPostId());
        System.out.println("Commenter ID: " + commentRequestDTO.getCommenterId());
        System.out.println("Comment Text: " + commentRequestDTO.getCommentText());
        
        MedicalPostComment medicalPostComment = commentMapStructRelated.commentRequestDTOToMedicalPostComment(commentRequestDTO);
        MedicalPost medicalPost = postRepository.findById(commentRequestDTO.getPostId()).get();
        medicalPostComment.setPost(medicalPost);
        medicalPostCommentRepository.save(medicalPostComment);
        medicalPostCommentRepository.flush(); // Force immediate database update
        
        // Create notification for post owner
        try {
            Users actor = userRepo.findById(commentRequestDTO.getCommenterId()).orElse(null);
            if (actor != null) {
                notificationService.createPostCommentNotification(actor, medicalPost, medicalPostComment);
                System.out.println("✓ Created comment notification");
            }
        } catch (Exception e) {
            System.err.println("Failed to create comment notification: " + e.getMessage());
        }
        
        System.out.println("Comment saved successfully with ID: " + medicalPostComment.getId());
    }

    @Override
    public List<Map<String, Object>> getCommentsByPostId(Long postId, Long userId) {
        System.out.println("=== GET COMMENTS FOR POST " + postId + " ===");
        List<MedicalPostComment> comments = medicalPostCommentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        List<Map<String, Object>> commentsWithDetails = new ArrayList<>();
        
        System.out.println("Found " + comments.size() + " comments");

        for (MedicalPostComment comment : comments) {
            Map<String, Object> commentDetails = new HashMap<>();
            commentDetails.put("id", comment.getId());
            commentDetails.put("content", comment.getContent());
            commentDetails.put("createdAt", comment.getCreatedAt());
            commentDetails.put("commenterId", comment.getCommenterId());
            
            // Get commenter details - check both healthcare providers and patients
            if (comment.getCommenterId() != null) {
                // Try healthcare provider first
                Optional<HealthcareProvider> providerOpt = healthcareProviderRepo.findById(comment.getCommenterId());
                if (providerOpt.isPresent()) {
                    HealthcareProvider provider = providerOpt.get();
                    commentDetails.put("commenterName", "Dr. " + provider.getFirstName() + " " + provider.getLastName());
                    // Get first specialization or default
                    String specialty = provider.getSpecializations() != null && !provider.getSpecializations().isEmpty() 
                        ? provider.getSpecializations().get(0).toString() 
                        : "General Practice";
                    commentDetails.put("commenterSpecialty", specialty);
                    System.out.println("Comment by doctor: " + commentDetails.get("commenterName"));
                } else {
                    // Try patient
                    Optional<Patient> patientOpt = patientRepo.findById(comment.getCommenterId());
                    if (patientOpt.isPresent()) {
                        Patient patient = patientOpt.get();
                        commentDetails.put("commenterName", patient.getFirstName() + " " + patient.getLastName());
                        commentDetails.put("commenterSpecialty", "Patient");
                        System.out.println("Comment by patient: " + commentDetails.get("commenterName"));
                    } else {
                        // Fallback
                        commentDetails.put("commenterName", "Unknown User");
                        commentDetails.put("commenterSpecialty", "User");
                        System.out.println("Comment by unknown user");
                    }
                }
            }
            
            // Count likes for this comment
            long likeCount = commentLikeRepository.countByCommentId(comment.getId());
            commentDetails.put("likes", (int) likeCount);
            
            // Check if current user liked this comment
            if (userId != null) {
                List<CommentLike> userLikes = commentLikeRepository.findByCommentIdAndLikeGiverId(comment.getId(), userId);
                commentDetails.put("isLiked", !userLikes.isEmpty());
            } else {
                commentDetails.put("isLiked", false);
            }
            
            // Count replies for this comment
            long replyCount = commentReplyRepository.countByCommentId(comment.getId());
            commentDetails.put("replyCount", (int) replyCount);
            
            // Get replies for this comment
            List<CommentReply> replies = commentReplyRepository.findByCommentIdOrderByCreatedAtAsc(comment.getId());
            List<Map<String, Object>> repliesWithDetails = new ArrayList<>();
            
            for (CommentReply reply : replies) {
                Map<String, Object> replyDetails = new HashMap<>();
                replyDetails.put("id", reply.getId());
                replyDetails.put("content", reply.getContent());
                replyDetails.put("createdAt", reply.getCreatedAt());
                replyDetails.put("replierId", reply.getReplierId());
                
                // Get replier details
                if (reply.getReplierId() != null) {
                    Optional<HealthcareProvider> providerOpt = healthcareProviderRepo.findById(reply.getReplierId());
                    if (providerOpt.isPresent()) {
                        HealthcareProvider provider = providerOpt.get();
                        replyDetails.put("replierName", "Dr. " + provider.getFirstName() + " " + provider.getLastName());
                        String specialty = provider.getSpecializations() != null && !provider.getSpecializations().isEmpty() 
                            ? provider.getSpecializations().get(0).toString() 
                            : "General Practice";
                        replyDetails.put("replierSpecialty", specialty);
                    } else {
                        Optional<Patient> patientOpt = patientRepo.findById(reply.getReplierId());
                        if (patientOpt.isPresent()) {
                            Patient patient = patientOpt.get();
                            replyDetails.put("replierName", patient.getFirstName() + " " + patient.getLastName());
                            replyDetails.put("replierSpecialty", "Patient");
                        }
                    }
                }
                
                // Count likes for this reply
                long replyLikeCount = commentReplyLikeRepository.countByReplyId(reply.getId());
                replyDetails.put("likes", (int) replyLikeCount);
                
                // Check if current user liked this reply
                if (userId != null) {
                    List<CommentReplyLike> userReplyLikes = commentReplyLikeRepository.findByReplyIdAndLikeGiverId(reply.getId(), userId);
                    replyDetails.put("isLiked", !userReplyLikes.isEmpty());
                } else {
                    replyDetails.put("isLiked", false);
                }
                
                repliesWithDetails.add(replyDetails);
            }
            
            commentDetails.put("replies", repliesWithDetails);
            commentsWithDetails.add(commentDetails);
        }
        
        System.out.println("=== END GET COMMENTS ===");
        return commentsWithDetails;
    }

    @Override
    @Transactional
    public boolean likeComment(Long commentId, Long userId) {
        System.out.println("=== LIKE/UNLIKE COMMENT ===");
        System.out.println("Comment ID: " + commentId + ", User ID: " + userId);
        
        List<CommentLike> existingLikes = commentLikeRepository.findByCommentIdAndLikeGiverId(commentId, userId);
        
        if (!existingLikes.isEmpty()) {
            // Unlike
            int deletedCount = commentLikeRepository.deleteByCommentIdAndLikeGiverId(commentId, userId);
            System.out.println("Deleted " + deletedCount + " comment like(s)");
            return false;
        } else {
            // Like
            Optional<MedicalPostComment> commentOpt = medicalPostCommentRepository.findById(commentId);
            if (commentOpt.isPresent()) {
                CommentLike like = new CommentLike();
                like.setComment(commentOpt.get());
                like.setLikeGiverId(userId);
                like.setCreatedAt(new Date());
                commentLikeRepository.save(like);
                commentLikeRepository.flush();
                System.out.println("Comment liked successfully");
                
                // Create notification for comment owner
                Users actor = userRepo.findById(userId).orElse(null);
                if (actor != null) {
                    notificationService.createCommentLikeNotification(actor, commentOpt.get());
                }
                
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        System.out.println("=== DELETE COMMENT ===");
        System.out.println("Comment ID: " + commentId + ", User ID: " + userId);
        
        Optional<MedicalPostComment> commentOpt = medicalPostCommentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            MedicalPostComment comment = commentOpt.get();
            
            // Check if user is the owner of the comment
            if (Objects.equals(comment.getCommenterId(), userId)) {
                medicalPostCommentRepository.delete(comment);
                medicalPostCommentRepository.flush();
                System.out.println("Comment deleted successfully");
            } else {
                throw new RuntimeException("You can only delete your own comments");
            }
        } else {
            throw new RuntimeException("Comment not found");
        }
    }

    @Override
    @Transactional
    public void replyToComment(Long commentId, Long replierId, String replyText) {
        System.out.println("=== REPLY TO COMMENT ===");
        System.out.println("Comment ID: " + commentId + ", Replier ID: " + replierId);
        
        Optional<MedicalPostComment> commentOpt = medicalPostCommentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            CommentReply reply = new CommentReply();
            reply.setComment(commentOpt.get());
            reply.setReplierId(replierId);
            reply.setContent(replyText);
            reply.setCreatedAt(new Date());
            commentReplyRepository.save(reply);
            commentReplyRepository.flush();
            System.out.println("Reply saved successfully with ID: " + reply.getId());
        } else {
            throw new RuntimeException("Comment not found");
        }
    }

    @Override
    @Transactional
    public boolean likeReply(Long replyId, Long userId) {
        System.out.println("=== LIKE/UNLIKE REPLY ===");
        System.out.println("Reply ID: " + replyId + ", User ID: " + userId);
        
        List<CommentReplyLike> existingLikes = commentReplyLikeRepository.findByReplyIdAndLikeGiverId(replyId, userId);
        
        if (!existingLikes.isEmpty()) {
            // Unlike
            int deletedCount = commentReplyLikeRepository.deleteByReplyIdAndLikeGiverId(replyId, userId);
            System.out.println("Deleted " + deletedCount + " reply like(s)");
            return false;
        } else {
            // Like
            Optional<CommentReply> replyOpt = commentReplyRepository.findById(replyId);
            if (replyOpt.isPresent()) {
                CommentReplyLike like = new CommentReplyLike();
                like.setReply(replyOpt.get());
                like.setLikeGiverId(userId);
                like.setCreatedAt(new Date());
                commentReplyLikeRepository.save(like);
                commentReplyLikeRepository.flush();
                System.out.println("Reply liked successfully");
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void deleteReply(Long replyId, Long userId) {
        System.out.println("=== DELETE REPLY ===");
        System.out.println("Reply ID: " + replyId + ", User ID: " + userId);
        
        Optional<CommentReply> replyOpt = commentReplyRepository.findById(replyId);
        if (replyOpt.isPresent()) {
            CommentReply reply = replyOpt.get();
            
            // Check if user is the owner of the reply
            if (Objects.equals(reply.getReplierId(), userId)) {
                commentReplyRepository.delete(reply);
                commentReplyRepository.flush();
                System.out.println("Reply deleted successfully");
            } else {
                throw new RuntimeException("You can only delete your own replies");
            }
        } else {
            throw new RuntimeException("Reply not found");
        }
    }

    @Override
    public List<Map<String, Object>> getCommentLikers(Long commentId) {
        System.out.println("=== GET COMMENT LIKERS ===");
        System.out.println("Comment ID: " + commentId);
        
        // Get all likes for the comment
        List<CommentLike> allLikes = commentLikeRepository.findByCommentId(commentId);
        
        List<Map<String, Object>> likersWithDetails = new ArrayList<>();
        
        System.out.println("Found " + allLikes.size() + " likers for comment");
        
        for (CommentLike like : allLikes) {
            Map<String, Object> likerDetails = new HashMap<>();
            likerDetails.put("userId", like.getLikeGiverId());
            likerDetails.put("likedAt", like.getCreatedAt());
            
            if (like.getLikeGiverId() != null) {
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
                    Optional<Patient> patientOpt = patientRepo.findById(like.getLikeGiverId());
                    if (patientOpt.isPresent()) {
                        Patient patient = patientOpt.get();
                        likerDetails.put("name", patient.getFirstName() + " " + patient.getLastName());
                        likerDetails.put("specialty", "Patient");
                        likerDetails.put("userType", "patient");
                    }
                }
            }
            
            likersWithDetails.add(likerDetails);
        }
        
        System.out.println("Returning " + likersWithDetails.size() + " comment likers");
        return likersWithDetails;
    }

    @Override
    public List<Map<String, Object>> getReplyLikers(Long replyId) {
        System.out.println("=== GET REPLY LIKERS ===");
        System.out.println("Reply ID: " + replyId);
        
        List<CommentReplyLike> allLikes = commentReplyLikeRepository.findByReplyId(replyId);
        
        List<Map<String, Object>> likersWithDetails = new ArrayList<>();
        
        System.out.println("Found " + allLikes.size() + " likers for reply");
        
        for (CommentReplyLike like : allLikes) {
            Map<String, Object> likerDetails = new HashMap<>();
            likerDetails.put("userId", like.getLikeGiverId());
            likerDetails.put("likedAt", like.getCreatedAt());
            
            if (like.getLikeGiverId() != null) {
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
                    Optional<Patient> patientOpt = patientRepo.findById(like.getLikeGiverId());
                    if (patientOpt.isPresent()) {
                        Patient patient = patientOpt.get();
                        likerDetails.put("name", patient.getFirstName() + " " + patient.getLastName());
                        likerDetails.put("specialty", "Patient");
                        likerDetails.put("userType", "patient");
                    }
                }
            }
            
            likersWithDetails.add(likerDetails);
        }
        
        System.out.println("Returning " + likersWithDetails.size() + " reply likers");
        return likersWithDetails;
    }
}
