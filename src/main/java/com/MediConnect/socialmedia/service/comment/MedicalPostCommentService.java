package com.MediConnect.socialmedia.service.comment;

import com.MediConnect.socialmedia.dto.CreateCommentRequestDTO;

public interface MedicalPostCommentService {
    void createMedicalPostComment(CreateCommentRequestDTO commentRequestDTO);
}
