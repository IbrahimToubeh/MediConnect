package com.MediConnect.socialmedia.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePostReportRequestDTO {
    private String reason;
    private String otherReason;
    private String details;
}

