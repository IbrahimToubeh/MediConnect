package com.MediConnect.socialmedia.service.post;


import com.MediConnect.socialmedia.dto.CreatePostRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPost;

import java.util.List;
import java.util.Map;

public interface MedicalPostService {

    void saveMedicalPost(CreatePostRequestDTO createPostRequestDTO);
    MedicalPost createPost(CreatePostRequestDTO createPostRequestDTO);
    List<Map<String, Object>> getAllPostsWithDetails();
    List<Map<String, Object>> getAllPostsWithDetails(Long userId);
    
    List<Map<String, Object>> getPostsByDoctor(Long doctorId, Long userId);
    
    boolean likePost(Long postId, Long userId);
    void deletePost(Long postId, Long userId);
    List<Map<String, Object>> getPostLikers(Long postId);
}
