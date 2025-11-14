package com.MediConnect.socialmedia.dto;

import com.MediConnect.socialmedia.entity.enums.PostPrivacy;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePostRequestDTO {
    private Long providerId;
    private String content;
    private String mediaUrl;  // Single media URL (for backward compatibility)
    private String mediaUrls;  // JSON array of media URLs
    private PostPrivacy privacy;
}
