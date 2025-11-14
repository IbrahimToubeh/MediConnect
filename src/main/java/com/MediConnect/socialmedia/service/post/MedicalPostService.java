package com.MediConnect.socialmedia.service.post;


import com.MediConnect.socialmedia.dto.CreatePostRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.service.post.dto.AdminPostFilter;

import java.util.List;
import java.util.Map;

public interface MedicalPostService {

    void saveMedicalPost(CreatePostRequestDTO createPostRequestDTO);
    MedicalPost createPost(CreatePostRequestDTO createPostRequestDTO);
    List<Map<String, Object>> getAllPostsWithDetails();
    List<Map<String, Object>> getAllPostsWithDetails(Long userId);
    /**
     * Retrieves all posts with pagination support.
     * If page and size are null, returns all posts (backward compatible).
     * 
     * @param userId The ID of the current user (can be null for anonymous users)
     * @param page Page number (0-indexed, null to return all)
     * @param size Page size (null to return all)
     * @return Map containing "data" (list of posts), "totalElements", "totalPages", "currentPage", "pageSize"
     */
    Map<String, Object> getAllPostsWithDetails(Long userId, Integer page, Integer size);
    
    List<Map<String, Object>> getPostsByDoctor(Long doctorId, Long userId);
    /**
     * Retrieves posts by doctor with pagination support.
     * If page and size are null, returns all posts (backward compatible).
     * 
     * @param doctorId The ID of the doctor
     * @param userId The ID of the current user (can be null for anonymous users)
     * @param page Page number (0-indexed, null to return all)
     * @param size Page size (null to return all)
     * @return Map containing "data" (list of posts), "totalElements", "totalPages", "currentPage", "pageSize"
     */
    Map<String, Object> getPostsByDoctor(Long doctorId, Long userId, Integer page, Integer size);
    
    boolean likePost(Long postId, Long userId);
    void deletePost(Long postId, Long userId);
    List<Map<String, Object>> getPostLikers(Long postId);
    void flagPost(Long postId, String reason);
    void unflagPost(Long postId);
    List<Map<String, Object>> getFlaggedPosts();
    void adminDeletePost(Long postId);
    List<Map<String, Object>> getAdminPostReports(Long postId);
    List<Map<String, Object>> getAllPostsForAdmin(AdminPostFilter filter);
    /**
     * Retrieves all posts for admin with pagination support.
     * If page and size are null, returns all posts (backward compatible).
     * 
     * @param filter Filter criteria (can be null to return all posts)
     * @param page Page number (0-indexed, null to return all)
     * @param size Page size (null to return all)
     * @return Map containing "data" (list of posts), "totalElements", "totalPages", "currentPage", "pageSize"
     */
    Map<String, Object> getAllPostsForAdmin(AdminPostFilter filter, Integer page, Integer size);
}
