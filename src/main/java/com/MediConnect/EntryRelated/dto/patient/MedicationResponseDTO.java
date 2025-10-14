package com.MediConnect.EntryRelated.dto.patient;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class MedicationResponseDTO {
    private Long id;
    private String medicationName;
    private String medicationDosage;
    private String medicationFrequency;
    private Date medicationStartDate;
    private Date medicationEndDate;
    private boolean inUse;
}
