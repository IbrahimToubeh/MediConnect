package com.MediConnect.socialmedia.service.post;


import com.MediConnect.socialmedia.dto.CreatePostRequestDTO;

public interface MedicalPostService {

    void saveMedicalPost(CreatePostRequestDTO createPostRequestDTO);
}
