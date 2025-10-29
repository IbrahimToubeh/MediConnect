package com.MediConnect.EntryRelated.dto.healthprovider;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkExperienceDTO {
    @NotBlank(message = "Organization name is required")
    private String organizationName;
    
    @NotBlank(message = "Role title is required")
    private String roleTitle;
    
    @NotBlank(message = "Start date is required")
    private String startDate;
    
    private String endDate;
    private boolean stillWorking;
}
