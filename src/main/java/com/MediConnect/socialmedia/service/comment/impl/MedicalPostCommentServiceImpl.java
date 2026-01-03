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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;

/**
 * Service implementation for managing medical post comments, replies, and likes.
 * Handles CRUD operations, notifications, and user interactions with comments.
 */
@Slf4j
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
    private final NotificationRepository notificationRepository;

    /**
     * Creates a new comment on a medical post.
     * The comment is automatically saved and a notification is sent to the post owner.
     * 
     * @param commentRequestDTO DTO containing post ID, commenter ID, and comment text
     */
    @Override
    @Transactional
    public void createMedicalPostComment(CreateCommentRequestDTO commentRequestDTO) {
        log.debug("Creating comment - Post ID: {}, Commenter ID: {}", 
            commentRequestDTO.getPostId(), commentRequestDTO.getCommenterId());
        
        // Convert DTO to entity and set the post relationship
        MedicalPostComment medicalPostComment = commentMapStructRelated.commentRequestDTOToMedicalPostComment(commentRequestDTO);
        MedicalPost medicalPost = postRepository.findById(commentRequestDTO.getPostId())
            .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + commentRequestDTO.getPostId()));
        medicalPostComment.setPost(medicalPost);
        
        // Save comment (transaction will commit automatically at method end)
        medicalPostCommentRepository.save(medicalPostComment);
        log.info("Comment created successfully - ID: {}, Post ID: {}", 
            medicalPostComment.getId(), commentRequestDTO.getPostId());
        
        // Create notification for post owner
        try {
            Users actor = userRepo.findById(commentRequestDTO.getCommenterId()).orElse(null);
            if (actor != null) {
                notificationService.createPostCommentNotification(actor, medicalPost, medicalPostComment);
                log.debug("Comment notification created for post owner");
            }
        } catch (Exception e) {
            log.error("Failed to create comment notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieves all comments for a specific post with full details including likes and replies.
     * 
     * @param postId The ID of the post to get comments for
     * @param userId The ID of the current user (for checking if they liked comments)
     * @return List of comment maps with all details (commenter info, likes, replies, etc.)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCommentsByPostId(Long postId, Long userId) {
        log.debug("Fetching comments for post ID: {}, User ID: {}", postId, userId);
        
        List<MedicalPostComment> comments = medicalPostCommentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        
        log.debug("Found {} comments for post {}", comments.size(), postId);

        // Build comment DTOs using helper method
        List<Map<String, Object>> commentsWithDetails = comments.stream()
            .map(comment -> buildCommentDTO(comment, userId))
            .collect(Collectors.toList());
        
        log.debug("Returning {} comments with details for post {}", commentsWithDetails.size(), postId);
        return commentsWithDetails;
    }

    /**
     * Toggles like status for a comment (like if not liked, unlike if already liked).
     * 
     * @param commentId The ID of the comment to like/unlike
     * @param userId The ID of the user performing the action
     * @return true if comment is now liked, false if unliked
     * @throws RuntimeException if comment is not found
     */
    @Override
    @Transactional
    public boolean likeComment(Long commentId, Long userId) {
        log.debug("Toggling like for comment ID: {}, User ID: {}", commentId, userId);
        
        // Check if user already liked this comment
        List<CommentLike> existingLikes = commentLikeRepository.findByCommentIdAndLikeGiverId(commentId, userId);
        
        if (!existingLikes.isEmpty()) {
            // Unlike - delete existing like
            int deletedCount = commentLikeRepository.deleteByCommentIdAndLikeGiverId(commentId, userId);
            // Note: No need for explicit flush() - @Transactional handles commit automatically
            log.info("Comment unliked - Comment ID: {}, User ID: {}, Deleted {} like(s)", 
                commentId, userId, deletedCount);
            return false; // Return false to indicate unliked
        } else {
            // Like - create new like
            MedicalPostComment comment = medicalPostCommentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("Comment not found with ID: {}", commentId);
                    return new RuntimeException("Comment not found with ID: " + commentId);
                });
            
            CommentLike like = new CommentLike();
            like.setComment(comment);
            like.setLikeGiverId(userId);
            like.setCreatedAt(new Date());
            commentLikeRepository.save(like);
            // Note: No need for explicit flush() - @Transactional handles commit automatically
            log.info("Comment liked - Comment ID: {}, User ID: {}, Like ID: {}", 
                commentId, userId, like.getId());
            
            // Create notification for comment owner
            Users actor = userRepo.findById(userId).orElse(null);
            if (actor != null) {
                notificationService.createCommentLikeNotification(actor, comment);
            }
            
            return true;
        }
    }

    /**
     * Deletes a comment if the user is the owner.
     * 
     * @param commentId The ID of the comment to delete
     * @param userId The ID of the user attempting to delete
     * @throws RuntimeException if comment not found or user is not the owner
     */
    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.debug("Attempting to delete comment ID: {} by user ID: {}", commentId, userId);
        
        MedicalPostComment comment = medicalPostCommentRepository.findById(commentId)
            .orElseThrow(() -> {
                log.error("Comment not found with ID: {}", commentId);
                return new RuntimeException("Comment not found");
            });
        
        // Verify user is the owner of the comment
        if (!Objects.equals(comment.getCommenterId(), userId)) {
            log.warn("User {} attempted to delete comment {} owned by user {}", 
                userId, commentId, comment.getCommenterId());
            throw new RuntimeException("You can only delete your own comments");
        }
        
        // Remove notifications tied to this comment before deleting (to avoid foreign key constraint violation)
        notificationRepository.deleteByComment(comment);
        
        // Delete comment (cascade will handle related likes and replies)
        medicalPostCommentRepository.delete(comment);
        // Note: No need for explicit flush() - @Transactional handles commit automatically
        log.info("Comment deleted successfully - Comment ID: {}, User ID: {}", commentId, userId);
    }

    /**
     * Admin-only method to delete any comment.
     * Removes associated notifications before deleting the comment.
     * 
     * @param commentId The ID of the comment to delete
     * @throws EntityNotFoundException if comment not found
     */
    @Override
    @Transactional
    public void adminDeleteComment(Long commentId) {
        log.debug("Admin deleting comment ID: {}", commentId);

        MedicalPostComment comment = medicalPostCommentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("Comment not found for admin delete - ID: {}", commentId);
                    return new EntityNotFoundException("Comment not found");
                });

        // Remove notifications tied to this comment before deleting
        notificationRepository.deleteByComment(comment);

        // Delete comment (cascade will handle related likes and replies)
        medicalPostCommentRepository.delete(comment);
        // Note: No need for explicit flush() - @Transactional handles commit automatically
        log.info("Admin deleted comment successfully - Comment ID: {}", commentId);
    }

    /**
     * Creates a reply to an existing comment.
     * 
     * @param commentId The ID of the comment being replied to
     * @param replierId The ID of the user creating the reply
     * @param replyText The content of the reply
     * @throws RuntimeException if comment not found
     */
    @Override
    @Transactional
    public void replyToComment(Long commentId, Long replierId, String replyText) {
        log.debug("Creating reply to comment ID: {} by user ID: {}", commentId, replierId);
        
        MedicalPostComment comment = medicalPostCommentRepository.findById(commentId)
            .orElseThrow(() -> {
                log.error("Comment not found for reply - ID: {}", commentId);
                return new RuntimeException("Comment not found");
            });
        
        CommentReply reply = new CommentReply();
        reply.setComment(comment);
        reply.setReplierId(replierId);
        reply.setContent(replyText);
        reply.setCreatedAt(new Date());
        commentReplyRepository.save(reply);
        // Note: No need for explicit flush() - @Transactional handles commit automatically
        log.info("Reply created successfully - Reply ID: {}, Comment ID: {}", reply.getId(), commentId);
    }

    /**
     * Toggles like status for a reply (like if not liked, unlike if already liked).
     * 
     * @param replyId The ID of the reply to like/unlike
     * @param userId The ID of the user performing the action
     * @return true if reply is now liked, false if unliked
     * @throws RuntimeException if reply is not found
     */
    @Override
    @Transactional
    public boolean likeReply(Long replyId, Long userId) {
        log.debug("Toggling like for reply ID: {}, User ID: {}", replyId, userId);
        
        // Check if user already liked this reply
        List<CommentReplyLike> existingLikes = commentReplyLikeRepository.findByReplyIdAndLikeGiverId(replyId, userId);
        
        if (!existingLikes.isEmpty()) {
            // Unlike - delete existing like
            int deletedCount = commentReplyLikeRepository.deleteByReplyIdAndLikeGiverId(replyId, userId);
            // Note: No need for explicit flush() - @Transactional handles commit automatically
            log.info("Reply unliked - Reply ID: {}, User ID: {}, Deleted {} like(s)", 
                replyId, userId, deletedCount);
            return false; // Return false to indicate unliked
        } else {
            // Like - create new like
            CommentReply reply = commentReplyRepository.findById(replyId)
                .orElseThrow(() -> {
                    log.error("Reply not found with ID: {}", replyId);
                    return new RuntimeException("Reply not found with ID: " + replyId);
                });
            
            CommentReplyLike like = new CommentReplyLike();
            like.setReply(reply);
            like.setLikeGiverId(userId);
            like.setCreatedAt(new Date());
            commentReplyLikeRepository.save(like);
            // Note: No need for explicit flush() - @Transactional handles commit automatically
            log.info("Reply liked - Reply ID: {}, User ID: {}, Like ID: {}", 
                replyId, userId, like.getId());
            return true;
        }
    }

    /**
     * Deletes a reply if the user is the owner.
     * 
     * @param replyId The ID of the reply to delete
     * @param userId The ID of the user attempting to delete
     * @throws RuntimeException if reply not found or user is not the owner
     */
    @Override
    @Transactional
    public void deleteReply(Long replyId, Long userId) {
        log.debug("Attempting to delete reply ID: {} by user ID: {}", replyId, userId);
        
        CommentReply reply = commentReplyRepository.findById(replyId)
            .orElseThrow(() -> {
                log.error("Reply not found with ID: {}", replyId);
                return new RuntimeException("Reply not found");
            });
        
        // Verify user is the owner of the reply
        if (!Objects.equals(reply.getReplierId(), userId)) {
            log.warn("User {} attempted to delete reply {} owned by user {}", 
                userId, replyId, reply.getReplierId());
            throw new RuntimeException("You can only delete your own replies");
        }
        
        // Delete reply (cascade will handle related likes)
        commentReplyRepository.delete(reply);
        // Note: No need for explicit flush() - @Transactional handles commit automatically
        log.info("Reply deleted successfully - Reply ID: {}, User ID: {}", replyId, userId);
    }

    /**
     * Retrieves all users who liked a specific comment with their details.
     * 
     * @param commentId The ID of the comment
     * @return List of maps containing liker details (name, specialty, userType, etc.)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCommentLikers(Long commentId) {
        log.debug("Fetching likers for comment ID: {}", commentId);
        
        // Get all likes for the comment
        List<CommentLike> allLikes = commentLikeRepository.findByCommentId(commentId);
        
        log.debug("Found {} likers for comment {}", allLikes.size(), commentId);
        
        // Build liker DTOs using helper method
        List<Map<String, Object>> likersWithDetails = allLikes.stream()
            .map(this::buildCommentLikerDetails)
            .collect(Collectors.toList());
        
        log.debug("Returning {} likers for comment {}", likersWithDetails.size(), commentId);
        return likersWithDetails;
    }

    /**
     * Retrieves all users who liked a specific reply with their details.
     * 
     * @param replyId The ID of the reply
     * @return List of maps containing liker details (name, specialty, userType, etc.)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReplyLikers(Long replyId) {
        log.debug("Fetching likers for reply ID: {}", replyId);
        
        List<CommentReplyLike> allLikes = commentReplyLikeRepository.findByReplyId(replyId);
        
        log.debug("Found {} likers for reply {}", allLikes.size(), replyId);
        
        // Build liker DTOs using helper method
        List<Map<String, Object>> likersWithDetails = allLikes.stream()
            .map(this::buildReplyLikerDetails)
            .collect(Collectors.toList());
        
        log.debug("Returning {} likers for reply {}", likersWithDetails.size(), replyId);
        return likersWithDetails;
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
        
        // First, try to find the user in the Users table to determine their role
        Optional<Users> userOpt = userRepo.findById(userId);
        if (userOpt.isPresent()) {
            Users user = userOpt.get();
            String role = user.getRole();
            
            // Based on role, look up in the appropriate table
            if ("HEALTHCARE_PROVIDER".equalsIgnoreCase(role) || "DOCTOR".equalsIgnoreCase(role)) {
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
            } else if ("PATIENT".equalsIgnoreCase(role)) {
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
            }
            
            // If role-based lookup failed, use basic user info as fallback
            userDetails.put("name", user.getFirstName() + " " + user.getLastName());
            userDetails.put("specialty", role != null ? role : "User");
            userDetails.put("userType", role != null ? role.toLowerCase() : "unknown");
            userDetails.put("profilePicture", null); // General users table doesn't have profile picture
            log.debug("Using basic user info for user ID {} with role {}", userId, role);
            return userDetails;
        }
        
        // Fallback: try direct lookup in both tables (for backward compatibility)
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
        
        // Final fallback for unknown users
        userDetails.put("name", "Unknown User");
        userDetails.put("specialty", "User");
        userDetails.put("userType", "unknown");
        userDetails.put("profilePicture", null);
        log.warn("User with ID {} not found in Users, healthcare providers, or patients", userId);
        return userDetails;
    }
    
    /**
     * Builds commenter/replier details map for a given user ID.
     * Used specifically for comment and reply DTOs (uses "commenterName" or "replierName" keys).
     * 
     * @param userId The ID of the user
     * @param prefix The prefix for the name key ("commenter" or "replier")
     * @return Map containing user details with prefixed keys
     */
    private Map<String, Object> buildCommenterDetails(Long userId, String prefix) {
        if (userId == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> details = new HashMap<>();
        
        // First, try to find the user in the Users table to determine their role
        Optional<Users> userOpt = userRepo.findById(userId);
        if (userOpt.isPresent()) {
            Users user = userOpt.get();
            String role = user.getRole();
            
            // Based on role, look up in the appropriate table
            if ("HEALTHCARE_PROVIDER".equalsIgnoreCase(role) || "DOCTOR".equalsIgnoreCase(role)) {
                Optional<HealthcareProvider> providerOpt = healthcareProviderRepo.findById(userId);
                if (providerOpt.isPresent()) {
                    HealthcareProvider provider = providerOpt.get();
                    details.put(prefix + "Name", "Dr. " + provider.getFirstName() + " " + provider.getLastName());
                    String specialty = provider.getSpecializations() != null && !provider.getSpecializations().isEmpty() 
                        ? provider.getSpecializations().get(0).toString() 
                        : "General Practice";
                    details.put(prefix + "Specialty", specialty);
                    // Add profile picture from Users table
                    details.put(prefix + "ProfilePicture", user.getProfilePicture());
                    return details;
                }
            } else if ("PATIENT".equalsIgnoreCase(role)) {
                Optional<Patient> patientOpt = patientRepo.findById(userId);
                if (patientOpt.isPresent()) {
                    Patient patient = patientOpt.get();
                    details.put(prefix + "Name", patient.getFirstName() + " " + patient.getLastName());
                    details.put(prefix + "Specialty", "Patient");
                    // Add profile picture from Users table
                    details.put(prefix + "ProfilePicture", user.getProfilePicture());
                    return details;
                }
            }
            
            // If role-based lookup failed, use basic user info as fallback
            // This handles cases like ADMIN or other roles that don't have specific tables
            String fullName = user.getFirstName() + " " + user.getLastName();
            details.put(prefix + "Name", fullName);
            // For admin or other roles, set specialty based on role
            if ("ADMIN".equalsIgnoreCase(role)) {
                details.put(prefix + "Specialty", "ADMIN");
            } else {
                details.put(prefix + "Specialty", role != null ? role : "User");
            }
            // Add profile picture from Users table
            details.put(prefix + "ProfilePicture", user.getProfilePicture());
            log.debug("Using basic user info for user ID {} with role {}: {}", userId, role, fullName);
            return details;
        }
        
        // Fallback: try direct lookup in both tables (for backward compatibility)
        Optional<HealthcareProvider> providerOpt = healthcareProviderRepo.findById(userId);
        if (providerOpt.isPresent()) {
            HealthcareProvider provider = providerOpt.get();
            details.put(prefix + "Name", "Dr. " + provider.getFirstName() + " " + provider.getLastName());
            String specialty = provider.getSpecializations() != null && !provider.getSpecializations().isEmpty() 
                ? provider.getSpecializations().get(0).toString() 
                : "General Practice";
            details.put(prefix + "Specialty", specialty);
            // Try to get profile picture from Users table
            if (userOpt.isPresent()) {
                details.put(prefix + "ProfilePicture", userOpt.get().getProfilePicture());
            }
            return details;
        }
        
        Optional<Patient> patientOpt = patientRepo.findById(userId);
        if (patientOpt.isPresent()) {
            Patient patient = patientOpt.get();
            details.put(prefix + "Name", patient.getFirstName() + " " + patient.getLastName());
            details.put(prefix + "Specialty", "Patient");
            // Try to get profile picture from Users table
            if (userOpt.isPresent()) {
                details.put(prefix + "ProfilePicture", userOpt.get().getProfilePicture());
            }
            return details;
        }
        
        // Final fallback for unknown users
        details.put(prefix + "Name", "Unknown User");
        details.put(prefix + "Specialty", "User");
        log.warn("User with ID {} not found in Users, healthcare providers, or patients", userId);
        return details;
    }
    
    /**
     * Builds a reply DTO map from a CommentReply entity.
     * 
     * @param reply The reply entity
     * @param userId The ID of the current user (for checking if they liked the reply)
     * @return Map containing reply details
     */
    private Map<String, Object> buildReplyDTO(CommentReply reply, Long userId) {
        Map<String, Object> replyDetails = new HashMap<>();
        replyDetails.put("id", reply.getId());
        replyDetails.put("content", reply.getContent());
        replyDetails.put("createdAt", reply.getCreatedAt());
        replyDetails.put("replierId", reply.getReplierId());
        
        // Get replier details
        Map<String, Object> replierDetails = buildCommenterDetails(reply.getReplierId(), "replier");
        replyDetails.putAll(replierDetails);
        
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
        
        return replyDetails;
    }
    
    /**
     * Builds a comment DTO map from a MedicalPostComment entity.
     * 
     * @param comment The comment entity
     * @param userId The ID of the current user (for checking if they liked the comment)
     * @return Map containing comment details including replies
     */
    private Map<String, Object> buildCommentDTO(MedicalPostComment comment, Long userId) {
        Map<String, Object> commentDetails = new HashMap<>();
        commentDetails.put("id", comment.getId());
        commentDetails.put("content", comment.getContent());
        commentDetails.put("createdAt", comment.getCreatedAt());
        commentDetails.put("commenterId", comment.getCommenterId());
        
        // Get commenter details
        Map<String, Object> commenterDetails = buildCommenterDetails(comment.getCommenterId(), "commenter");
        commentDetails.putAll(commenterDetails);
        
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
        List<Map<String, Object>> repliesWithDetails = replies.stream()
            .map(reply -> buildReplyDTO(reply, userId))
            .collect(Collectors.toList());
        
        commentDetails.put("replies", repliesWithDetails);
        
        return commentDetails;
    }
    
    /**
     * Builds a liker details map from a CommentLike entity.
     * 
     * @param like The like entity
     * @return Map containing liker details (userId, likedAt, name, specialty, userType)
     */
    private Map<String, Object> buildCommentLikerDetails(CommentLike like) {
        Map<String, Object> likerDetails = new HashMap<>();
        likerDetails.put("userId", like.getLikeGiverId());
        likerDetails.put("likedAt", like.getCreatedAt());
        
        // Get user details
        Map<String, Object> userDetails = buildUserDetails(like.getLikeGiverId());
        if (userDetails != null) {
            likerDetails.putAll(userDetails);
        }
        
        return likerDetails;
    }
    
    /**
     * Builds a liker details map from a CommentReplyLike entity.
     * 
     * @param like The like entity
     * @return Map containing liker details (userId, likedAt, name, specialty, userType)
     */
    private Map<String, Object> buildReplyLikerDetails(CommentReplyLike like) {
        Map<String, Object> likerDetails = new HashMap<>();
        likerDetails.put("userId", like.getLikeGiverId());
        likerDetails.put("likedAt", like.getCreatedAt());
        
        // Get user details
        Map<String, Object> userDetails = buildUserDetails(like.getLikeGiverId());
        if (userDetails != null) {
            likerDetails.putAll(userDetails);
        }
        
        return likerDetails;
    }
}
