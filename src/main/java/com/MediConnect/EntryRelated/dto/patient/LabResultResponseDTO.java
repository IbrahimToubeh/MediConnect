package com.MediConnect.EntryRelated.dto.patient;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabResultResponseDTO {
    private Long id;
    private String description;
    private boolean hasImage;
    private int imageSize;
    private String resultUrl;
}
