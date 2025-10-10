package com.MediConnect.socialmedia.service.comment;

import com.MediConnect.socialmedia.dto.CreateCommentRequestDTO;

import java.util.List;
import java.util.Map;

public interface MedicalPostCommentService {
    void createMedicalPostComment(CreateCommentRequestDTO commentRequestDTO);
    List<Map<String, Object>> getCommentsByPostId(Long postId);
}
