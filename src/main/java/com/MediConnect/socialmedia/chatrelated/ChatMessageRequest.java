package com.MediConnect.socialmedia.chatrelated;


import lombok.Data;

@Data
public class ChatMessageRequest {
    private Long channelId;
    private String content;
    private String senderUsername; // We'll trust this or extract from Principal in advanced setups
}