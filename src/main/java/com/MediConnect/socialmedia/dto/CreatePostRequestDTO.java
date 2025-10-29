package com.MediConnect.socialmedia.dto;

import com.MediConnect.socialmedia.entity.enums.PostPrivacy;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePostRequestDTO {
    private Long providerId;
    private String content;
    private String mediaUrl;
    private PostPrivacy privacy;
}
