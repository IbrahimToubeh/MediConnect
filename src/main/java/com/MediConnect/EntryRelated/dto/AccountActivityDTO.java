package com.MediConnect.EntryRelated.dto;

import com.MediConnect.EntryRelated.entities.AccountActivity;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountActivityDTO {
    
    private Long id;
    private AccountActivity.ActivityType type;
    private String description;
    private String ipAddress;
    private String location;
    private String device;
    private String userAgent;
    private LocalDateTime timestamp;
    private String additionalData;
}
