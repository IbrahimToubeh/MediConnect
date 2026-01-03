package com.MediConnect.EntryRelated.dto.healthprovider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockedTimeSlotDTO {
    private String date; // YYYY-MM-DD format
    private String startTime; // HH:mm format
    private String endTime; // HH:mm format
    private String reason; // Optional reason
}

