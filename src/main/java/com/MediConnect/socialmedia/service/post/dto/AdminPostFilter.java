package com.MediConnect.socialmedia.service.post.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class AdminPostFilter {
    private String doctorName;
    private Boolean reportedOnly;
    private Date startDate;
    private Date endDate;
    private Boolean flaggedOnly;
}

