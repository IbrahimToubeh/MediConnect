package com.MediConnect.socialmedia.service.comment.impl;

import com.MediConnect.socialmedia.dto.CreateCommentRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.entity.MedicalPostComment;
import com.MediConnect.socialmedia.repository.MedicalPostCommentRepository;
import com.MediConnect.socialmedia.repository.MedicalPostRepository;
import com.MediConnect.socialmedia.service.comment.MedicalPostCommentService;
import com.MediConnect.socialmedia.service.comment.mapper.CommentMapStructRelated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicalPostCommentServiceImpl implements MedicalPostCommentService {
    private final MedicalPostCommentRepository medicalPostCommentRepository;
    private final CommentMapStructRelated commentMapStructRelated;
    private final MedicalPostRepository postRepository;
    @Override
    public void createMedicalPostComment(CreateCommentRequestDTO commentRequestDTO) {
        MedicalPostComment medicalPostComment = commentMapStructRelated.commentRequestDTOToMedicalPostComment(commentRequestDTO);
        MedicalPost medicalPost = postRepository.findById(commentRequestDTO.getPostId()).get();
        medicalPostComment.setPost(medicalPost);
        medicalPostCommentRepository.save(medicalPostComment);
    }
}
