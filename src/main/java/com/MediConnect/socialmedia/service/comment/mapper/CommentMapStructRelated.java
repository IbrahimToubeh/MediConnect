package com.MediConnect.socialmedia.service.comment.mapper;

import com.MediConnect.socialmedia.dto.CreateCommentRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPostComment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapStructRelated {

    @Mapping(target = "content",source = "commentText")
    MedicalPostComment commentRequestDTOToMedicalPostComment(CreateCommentRequestDTO commentRequestDTO);
}
