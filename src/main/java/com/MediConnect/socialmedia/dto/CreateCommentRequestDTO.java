package com.MediConnect.socialmedia.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequestDTO {
    private Long commenterId;
    private Long postId;
    private String commentText;
}
