package com.MediConnect.EntryRelated.dto.healthprovider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleUpdateDTO {
    private List<DayAvailabilityDTO> dayAvailabilities;
    private Integer appointmentDurationMinutes; // 15, 30, 45, 60, etc.
}

