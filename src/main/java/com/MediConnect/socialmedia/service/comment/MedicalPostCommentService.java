package com.MediConnect.socialmedia.service.comment;

import com.MediConnect.socialmedia.dto.CreateCommentRequestDTO;

import java.util.List;
import java.util.Map;

public interface MedicalPostCommentService {
    void createMedicalPostComment(CreateCommentRequestDTO commentRequestDTO);
    List<Map<String, Object>> getCommentsByPostId(Long postId, Long userId);
    boolean likeComment(Long commentId, Long userId);
    void deleteComment(Long commentId, Long userId);
    void adminDeleteComment(Long commentId);
    void replyToComment(Long commentId, Long replierId, String replyText);
    boolean likeReply(Long replyId, Long userId);
    void deleteReply(Long replyId, Long userId);
    List<Map<String, Object>> getCommentLikers(Long commentId);
    List<Map<String, Object>> getReplyLikers(Long replyId);
}
