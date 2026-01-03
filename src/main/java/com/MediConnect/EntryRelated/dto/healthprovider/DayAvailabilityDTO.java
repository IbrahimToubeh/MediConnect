package com.MediConnect.EntryRelated.dto.healthprovider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayAvailabilityDTO {
    private String dayOfWeek; // "Monday", "Tuesday", etc.
    private Boolean enabled;
    private String startTime; // HH:mm format
    private String endTime; // HH:mm format
}

