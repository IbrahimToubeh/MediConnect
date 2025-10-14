package com.MediConnect.EntryRelated.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginSessionDTO {
    
    private Long id;
    private String sessionToken;
    private String ipAddress;
    private String userAgent;
    private String location;
    private String device;
    private String browser;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private Boolean isActive;
    private LocalDateTime lastActivityTime;
    private Boolean isCurrentSession;
}
