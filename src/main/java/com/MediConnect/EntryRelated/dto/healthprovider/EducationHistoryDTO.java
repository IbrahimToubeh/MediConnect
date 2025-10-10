package com.MediConnect.EntryRelated.dto.healthprovider;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EducationHistoryDTO {
    @NotBlank(message = "Institution name is required")
    private String institutionName;
    
    @NotBlank(message = "Start date is required")
    private String startDate;
    
    private String endDate;
    private boolean stillEnrolled;
}
