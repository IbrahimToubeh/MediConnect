package com.MediConnect.socialmedia.controller;


import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.EntryRelated.repository.PatientRepo;
import com.MediConnect.config.JWTService;
import com.MediConnect.socialmedia.dto.CreateCommentRequestDTO;
import com.MediConnect.socialmedia.dto.CreatePostRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.service.comment.MedicalPostCommentService;
import com.MediConnect.socialmedia.service.post.MedicalPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
//todo: add delete comment for the commenter
public class MedicalPostController {

    private final MedicalPostService medicalPostService;
    private final MedicalPostCommentService medicalPostCommentService;
    private final JWTService jwtService;
    private final HealthcareProviderRepo healthcareProviderRepo;
    private final PatientRepo patientRepo;

    @GetMapping("/feed")
    public ResponseEntity<List<Map<String, Object>>> getFeed(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = null;
            if (token != null && !token.isEmpty()) {
                try {
                    userId = extractUserIdFromToken(token);
                } catch (Exception e) {
                    System.out.println("Failed to extract user ID from token: " + e.getMessage());
                }
            }
            
            List<Map<String, Object>> posts = medicalPostService.getAllPostsWithDetails(userId);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody CreatePostRequestDTO postRequest) {
        try {
            MedicalPost post = medicalPostService.createPost(postRequest);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Post created successfully",
                "postId", post.getId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to create post: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/like/{postId}")
    public ResponseEntity<Map<String, Object>> likePost(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String token) {
        try {
            // Extract user ID from token
            Long userId = extractUserIdFromToken(token);
            boolean isLiked = medicalPostService.likePost(postId, userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", isLiked ? "Post liked successfully" : "Post unliked successfully",
                "isLiked", isLiked
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to like post: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/comment")
    public ResponseEntity<Map<String, Object>> addComment(@RequestBody CreateCommentRequestDTO commentRequest) {
        try {
            medicalPostCommentService.createMedicalPostComment(commentRequest);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Comment added successfully"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to add comment: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/comments/{postId}")
    public ResponseEntity<List<Map<String, Object>>> getComments(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = null;
            if (token != null && !token.isEmpty()) {
                try {
                    userId = extractUserIdFromToken(token);
                } catch (Exception e) {
                    System.out.println("Failed to extract user ID: " + e.getMessage());
                }
            }
            
            List<Map<String, Object>> comments = medicalPostCommentService.getCommentsByPostId(postId, userId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/comment/like/{commentId}")
    public ResponseEntity<Map<String, Object>> likeComment(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            boolean isLiked = medicalPostCommentService.likeComment(commentId, userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", isLiked ? "Comment liked successfully" : "Comment unliked successfully",
                "isLiked", isLiked
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to like comment: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            medicalPostCommentService.deleteComment(commentId, userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Comment deleted successfully"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to delete comment: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/comment/reply")
    public ResponseEntity<Map<String, Object>> replyToComment(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            Long commentId = Long.valueOf(request.get("commentId").toString());
            String replyText = request.get("replyText").toString();
            
            medicalPostCommentService.replyToComment(commentId, userId, replyText);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Reply added successfully"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to add reply: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/reply/like/{replyId}")
    public ResponseEntity<Map<String, Object>> likeReply(
            @PathVariable Long replyId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            boolean isLiked = medicalPostCommentService.likeReply(replyId, userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", isLiked ? "Reply liked successfully" : "Reply unliked successfully",
                "isLiked", isLiked
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to like reply: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/reply/{replyId}")
    public ResponseEntity<Map<String, Object>> deleteReply(
            @PathVariable Long replyId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            medicalPostCommentService.deleteReply(replyId, userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Reply deleted successfully"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to delete reply: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> deletePost(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String token) {
        try {
            // Extract user ID from token (you'll need to implement this)
            Long userId = extractUserIdFromToken(token);
            medicalPostService.deletePost(postId, userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Post deleted successfully"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to delete post: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{postId}/likers")
    public ResponseEntity<List<Map<String, Object>>> getPostLikers(@PathVariable Long postId) {
        try {
            List<Map<String, Object>> likers = medicalPostService.getPostLikers(postId);
            return ResponseEntity.ok(likers);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/comment/{commentId}/likers")
    public ResponseEntity<List<Map<String, Object>>> getCommentLikers(@PathVariable Long commentId) {
        try {
            List<Map<String, Object>> likers = medicalPostCommentService.getCommentLikers(commentId);
            return ResponseEntity.ok(likers);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reply/{replyId}/likers")
    public ResponseEntity<List<Map<String, Object>>> getReplyLikers(@PathVariable Long replyId) {
        try {
            List<Map<String, Object>> likers = medicalPostCommentService.getReplyLikers(replyId);
            return ResponseEntity.ok(likers);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Map<String, Object>>> getPostsByDoctor(
            @PathVariable Long doctorId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = null;
            if (token != null && !token.isEmpty()) {
                try {
                    userId = extractUserIdFromToken(token);
                } catch (Exception e) {
                    // If token extraction fails, just return posts without user-specific data
                    System.out.println("Failed to extract user ID from token: " + e.getMessage());
                }
            }
            
            List<Map<String, Object>> posts = medicalPostService.getPostsByDoctor(doctorId, userId);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private Long extractUserIdFromToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // Extract username from JWT token
            String username = jwtService.extractUserName(token);
            
            // Try to find healthcare provider first
            Optional<HealthcareProvider> provider = healthcareProviderRepo.findByUsername(username);
            if (provider.isPresent()) {
                return provider.get().getId();
            }
            
            // If not a provider, try to find patient
            Optional<Patient> patient = patientRepo.findByUsername(username);
            if (patient.isPresent()) {
                return patient.get().getId();
            }
            
            // If user not found, throw exception
            throw new RuntimeException("User not found for username: " + username);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to extract user ID from token: " + e.getMessage());
        }
    }
}
