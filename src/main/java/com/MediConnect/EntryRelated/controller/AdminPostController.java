package com.MediConnect.EntryRelated.controller;

import com.MediConnect.socialmedia.service.comment.MedicalPostCommentService;
import com.MediConnect.socialmedia.service.post.MedicalPostService;
import com.MediConnect.socialmedia.service.post.dto.AdminPostFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminPostController {

    private final MedicalPostService medicalPostService;
    private final MedicalPostCommentService medicalPostCommentService;

    @GetMapping("/flagged")
    public ResponseEntity<Map<String, Object>> getFlaggedPosts() {
        List<Map<String, Object>> posts = medicalPostService.getFlaggedPosts();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("count", posts.size());
        response.put("posts", posts);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all posts for admin with optional filtering and pagination.
     * If page and size are not provided, defaults to first page with 20 posts for better performance.
     * 
     * @param doctorName Filter by doctor name (optional)
     * @param reportedOnly Filter to show only reported posts (optional)
     * @param startDate Filter posts from this date (optional)
     * @param endDate Filter posts until this date (optional)
     * @param flaggedOnly Filter to show only flagged posts (optional)
     * @param page Page number (0-indexed, optional, defaults to 0)
     * @param size Page size (optional, defaults to 20)
     * @return Paginated response with posts and pagination metadata
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllPosts(
            @RequestParam(value = "doctorName", required = false) String doctorName,
            @RequestParam(value = "reportedOnly", required = false) Boolean reportedOnly,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(value = "flaggedOnly", required = false) Boolean flaggedOnly,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "End date must be after start date"
            ));
        }

        AdminPostFilter filter = new AdminPostFilter();
        if (StringUtils.hasText(doctorName)) {
            filter.setDoctorName(doctorName.trim());
        }
        filter.setReportedOnly(reportedOnly);
        filter.setFlaggedOnly(flaggedOnly);

        if (startDate != null) {
            Date start = Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant());
            filter.setStartDate(start);
        }

        if (endDate != null) {
            Date end = Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant());
            filter.setEndDate(end);
        }

        // Default pagination: first page, 20 posts per page (for better performance)
        // If page/size are explicitly provided, use them; otherwise use defaults
        int pageNum = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 20;
        
        Map<String, Object> paginatedResponse = medicalPostService.getAllPostsForAdmin(filter, pageNum, pageSize);
        
        // Wrap in admin response format for backward compatibility
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("count", paginatedResponse.get("totalElements"));
        response.put("posts", paginatedResponse.get("data"));
        
        // Include pagination metadata if pagination is used
        if (paginatedResponse.containsKey("totalPages")) {
            response.put("pagination", Map.of(
                "totalElements", paginatedResponse.get("totalElements"),
                "totalPages", paginatedResponse.get("totalPages"),
                "currentPage", paginatedResponse.get("currentPage"),
                "pageSize", paginatedResponse.get("pageSize"),
                "hasNext", paginatedResponse.get("hasNext"),
                "hasPrevious", paginatedResponse.get("hasPrevious")
            ));
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/flag")
    public ResponseEntity<Map<String, Object>> flagPost(
            @PathVariable Long postId,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            String reason = Optional.ofNullable(payload)
                    .map(body -> body.getOrDefault("reason", null))
                    .orElse(null);
            medicalPostService.flagPost(postId, reason);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Post flagged successfully");
            response.put("flagReason", reason);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to flag post {}: {}", postId, ex.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/{postId}/flag")
    public ResponseEntity<Map<String, Object>> unflagPost(@PathVariable Long postId) {
        try {
            medicalPostService.unflagPost(postId);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Post unflagged successfully");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to unflag post {}: {}", postId, ex.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable Long postId) {
        try {
            medicalPostService.adminDeletePost(postId);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Post deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to delete post {}: {}", postId, ex.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable Long commentId) {
        try {
            medicalPostCommentService.adminDeleteComment(commentId);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Comment deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to delete comment {}: {}", commentId, ex.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/{postId}/reports")
    public ResponseEntity<Map<String, Object>> getPostReports(@PathVariable Long postId) {
        try {
            List<Map<String, Object>> reports = medicalPostService.getAdminPostReports(postId);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", reports.size());
            response.put("reports", reports);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to fetch reports for post {}: {}", postId, ex.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}

