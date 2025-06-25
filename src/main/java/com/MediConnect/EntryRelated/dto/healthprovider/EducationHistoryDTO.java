package com.MediConnect.EntryRelated.dto.healthprovider;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class EducationHistoryDTO {
    private String institutionName;
    private Date startDate;
    private Date endDate;
    private boolean stillEnrolled;
}
