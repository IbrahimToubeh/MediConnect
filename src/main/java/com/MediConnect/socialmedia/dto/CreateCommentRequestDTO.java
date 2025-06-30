package com.MediConnect.socialmedia.dto;

import lombok.Getter;

@Getter
public class CreateCommentRequestDTO {
    private Long commenterId;
    private Long postId;
    private String commentText;
}
