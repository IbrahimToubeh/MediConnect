package com.MediConnect.socialmedia.controller;


import com.MediConnect.util.JwtTokenExtractor;
import com.MediConnect.util.JwtTokenExtractor.UserContext;
import com.MediConnect.socialmedia.dto.CreateCommentRequestDTO;
import com.MediConnect.socialmedia.dto.CreatePostReportRequestDTO;
import com.MediConnect.socialmedia.dto.CreatePostRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.entity.enums.PostReportReason;
import com.MediConnect.socialmedia.entity.enums.PostReporterType;
import com.MediConnect.socialmedia.entity.enums.PostPrivacy;
import com.MediConnect.socialmedia.service.CloudinaryService;
import com.MediConnect.socialmedia.service.comment.MedicalPostCommentService;
import com.MediConnect.socialmedia.service.post.MedicalPostService;
import com.MediConnect.socialmedia.service.report.MedicalPostReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
//todo: add delete comment for the commenter
public class MedicalPostController {

    private final MedicalPostService medicalPostService;
    private final MedicalPostCommentService medicalPostCommentService;
    private final MedicalPostReportService medicalPostReportService;
    private final JwtTokenExtractor jwtTokenExtractor;
    private final CloudinaryService cloudinaryService;

    /**
     * Test endpoint to verify Cloudinary connection
     * This endpoint checks if Cloudinary is properly configured and credentials are valid
     */
    @GetMapping("/test-cloudinary")
    public ResponseEntity<Map<String, Object>> testCloudinary() {
        try {
            // Test Cloudinary connection
            boolean connectionOk = cloudinaryService.testConnection();
            
            if (connectionOk) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Cloudinary connection successful! Ready to upload images.",
                    "cloudName", "dumzzhqnj"
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Cloudinary connection test failed. Please check your credentials in application.properties"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Cloudinary configuration error: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all posts feed with optional pagination.
     * If page and size are not provided, defaults to first page with 20 posts for better performance.
     * 
     * @param token Authorization token (optional)
     * @param page Page number (0-indexed, optional, defaults to 0)
     * @param size Page size (optional, defaults to 20)
     * @return Paginated response with posts and pagination metadata
     */
    @GetMapping("/feed")
    public ResponseEntity<?> getFeed(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        try {
            Long userId = null;
            if (token != null && !token.isEmpty()) {
                try {
                    userId = extractUserIdFromToken(token);
                } catch (Exception e) {
                    System.out.println("Failed to extract user ID from token: " + e.getMessage());
                }
            }
            
            // Default pagination: first page, 20 posts per page (for better performance)
            // If page/size are explicitly provided, use them; otherwise use defaults
            int pageNum = (page != null) ? page : 0;
            int pageSize = (size != null) ? size : 20;
            
            Map<String, Object> response = medicalPostService.getAllPostsWithDetails(userId, pageNum, pageSize);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a new medical post with optional image/video upload
     * 
     * Extracts the authenticated doctor from the JWT token and creates a post on their behalf.
     * If a media file is provided, it will be uploaded to Cloudinary and the URL will be stored.
     * 
     * @param content Post content text
     * @param privacy Post privacy setting (PUBLIC, DOCTORS_ONLY, PRIVATE)
     * @param mediaFile Optional image or video file to upload
     * @param authHeader JWT authorization token
     * @return Response with post creation status
     */
    @PostMapping(value = "/create", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> createPost(
            @RequestParam("content") String content,
            @RequestParam(value = "privacy", required = false, defaultValue = "PUBLIC") String privacy,
            @RequestParam(value = "mediaFiles", required = false) MultipartFile[] mediaFiles,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        try {
            // Try to get Authorization header from request if not in parameter
            if (authHeader == null || authHeader.isEmpty()) {
                authHeader = request.getHeader("Authorization");
            }
            
            // Extract doctor from JWT token for security
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Authorization token required"
                ));
            }
            
            // Extract user context from token to verify user type
            UserContext userContext = extractUserContextFromToken(authHeader);
            
            // Verify the user is a healthcare provider (only doctors can create posts)
            if (userContext.type() != PostReporterType.DOCTOR) {
                return ResponseEntity.status(403).body(Map.of(
                    "status", "error",
                    "message", "Only healthcare providers can create posts"
                ));
            }
            
            Long providerId = userContext.id();
            
            // Validate content
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Post content cannot be empty"
                ));
            }
            
            // Validate file count (max 10 files)
            if (mediaFiles != null && mediaFiles.length > 10) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Maximum 10 media files allowed per post"
                ));
            }
            
            // Upload media files to Cloudinary
            java.util.List<String> uploadedUrls = new java.util.ArrayList<>();
            String singleMediaUrl = null; // For backward compatibility
            
            if (mediaFiles != null && mediaFiles.length > 0) {
                for (MultipartFile file : mediaFiles) {
                    if (file != null && !file.isEmpty()) {
                        try {
                            String url = cloudinaryService.uploadFile(file);
                            uploadedUrls.add(url);
                            // Set first file as singleMediaUrl for backward compatibility
                            if (singleMediaUrl == null) {
                                singleMediaUrl = url;
                            }
                        } catch (Exception e) {
                            return ResponseEntity.badRequest().body(Map.of(
                                "status", "error",
                                "message", "Failed to upload media file: " + file.getOriginalFilename() + " - " + e.getMessage()
                            ));
                        }
                    }
                }
            }
            
            // Convert list to JSON array string
            String mediaUrlsJson = null;
            if (!uploadedUrls.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                mediaUrlsJson = objectMapper.writeValueAsString(uploadedUrls);
            }
            
            // Create post request DTO
            CreatePostRequestDTO postRequest = new CreatePostRequestDTO();
            postRequest.setProviderId(providerId);
            postRequest.setContent(content.trim());
            postRequest.setMediaUrl(singleMediaUrl); // For backward compatibility
            postRequest.setMediaUrls(mediaUrlsJson); // JSON array of all URLs
            // All posts are public
            postRequest.setPrivacy(PostPrivacy.PUBLIC);
            
            // Create the post
            MedicalPost post = medicalPostService.createPost(postRequest);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Post created successfully",
                "postId", post.getId(),
                "mediaUrls", uploadedUrls
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid request: " + e.getMessage()
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
    public ResponseEntity<Map<String, Object>> addComment(
            @RequestBody CreateCommentRequestDTO commentRequest,
            @RequestHeader("Authorization") String token) {
        try {
            // Extract user ID from token instead of trusting the request body
            Long userId = extractUserIdFromToken(token);
            
            // Override the commenterId from the request with the authenticated user's ID
            commentRequest.setCommenterId(userId);
            
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

    @PostMapping("/{postId}/report")
    public ResponseEntity<Map<String, Object>> reportPost(
            @PathVariable Long postId,
            @RequestBody CreatePostReportRequestDTO reportRequest,
            @RequestHeader("Authorization") String token) {
        try {
            if (token == null || token.isBlank()) {
                return ResponseEntity.status(401).body(Map.of(
                        "status", "error",
                        "message", "Authorization token is required"
                ));
            }

            UserContext userContext = extractUserContextFromToken(token);

            if (reportRequest == null || reportRequest.getReason() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Report reason is required"
                ));
            }

            PostReportReason reason = parseReportReason(reportRequest.getReason());

            medicalPostReportService.submitReport(
                    postId,
                    userContext.id(),
                    userContext.type(),
                    reason,
                    reportRequest.getOtherReason(),
                    reportRequest.getDetails()
            );

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Report submitted successfully"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to report post: " + e.getMessage()
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

    /**
     * Get posts by a specific doctor with optional pagination.
     * If page and size are not provided, defaults to first page with 20 posts for better performance.
     * 
     * @param doctorId The ID of the doctor
     * @param token Authorization token (optional)
     * @param page Page number (0-indexed, optional, defaults to 0)
     * @param size Page size (optional, defaults to 20)
     * @return Paginated response with posts and pagination metadata
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getPostsByDoctor(
            @PathVariable Long doctorId,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
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
            
            // Default pagination: first page, 20 posts per page (for better performance)
            // If page/size are explicitly provided, use them; otherwise use defaults
            int pageNum = (page != null) ? page : 0;
            int pageSize = (size != null) ? size : 20;
            
            Map<String, Object> response = medicalPostService.getPostsByDoctor(doctorId, userId, pageNum, pageSize);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Extracts user ID from JWT token using the centralized utility.
     * 
     * @param token The JWT token (with or without "Bearer " prefix)
     * @return The user ID
     */
    private Long extractUserIdFromToken(String token) {
        return jwtTokenExtractor.extractUserIdFromToken(token);
    }

    /**
     * Extracts user context (ID and type) from JWT token using the centralized utility.
     * 
     * @param token The JWT token (with or without "Bearer " prefix)
     * @return UserContext containing user ID and PostReporterType
     */
    private UserContext extractUserContextFromToken(String token) {
        return jwtTokenExtractor.extractUserContextFromToken(token);
    }

    private PostReportReason parseReportReason(String rawReason) {
        try {
            return PostReportReason.valueOf(rawReason.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid report reason: " + rawReason);
        }
    }

    /**
     * Exception handler for file upload size exceeded errors
     * Returns a user-friendly JSON error response
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        System.err.println("File upload size exceeded: " + ex.getMessage());
        return ResponseEntity.status(413).body(Map.of(
            "status", "error",
            "message", "File size exceeds the maximum allowed limit of 10MB per file. Please reduce the file size and try again."
        ));
    }
}
