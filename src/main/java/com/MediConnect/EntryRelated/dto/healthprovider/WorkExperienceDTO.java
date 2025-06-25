package com.MediConnect.EntryRelated.dto.healthprovider;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class WorkExperienceDTO {
    private String organizationName;
    private String roleTitle;
    private Date startDate;
    private Date endDate;
    private boolean stillWorking;
}
