package com.MediConnect.socialmedia.videorelated;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoSignal {
    private String type;      // "offer", "answer", or "ice-candidate"
    private String sdp;       // Session Description Protocol (Connection technical details)
    private Object candidate; // Network candidate details
    private String sender;    // Who sent this? (e.g., "doctor" or "patient")
    private String recipient; // Who should receive this?
}