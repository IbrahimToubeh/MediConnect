package com.MediConnect.socialmedia.service.post.mapper;

import com.MediConnect.socialmedia.dto.CreatePostRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPost;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapStructRelated {

    @Mapping(target = "postProvider", ignore = true)
    MedicalPost createPostRequestDTOToMedicalPost(CreatePostRequestDTO createPostRequestDTO);


}
