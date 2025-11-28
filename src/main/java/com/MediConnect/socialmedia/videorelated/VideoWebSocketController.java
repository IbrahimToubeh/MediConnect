package com.MediConnect.socialmedia.videorelated;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class VideoWebSocketController {

    /**
     * Endpoint: /app/video/{appointmentId}
     * Broadcasts to: /topic/video/{appointmentId}
     * * Handles: "offer", "answer", "ice-candidate"
     */
    @MessageMapping("/video/{appointmentId}")
    @SendTo("/topic/video/{appointmentId}")
    public VideoSignal handleVideoSignal(
            @DestinationVariable String appointmentId,
            @Payload VideoSignal signal,
            Principal principal
    ) {
        // Enforce that the 'sender' field matches the authenticated user
        // This prevents logic errors on the frontend
        if (principal != null) {
            signal.setSender(principal.getName());
        }

        log.info("Video Signal [{}] for Appointment {} from {}",
                signal.getType(), appointmentId, signal.getSender());

        // The frontend subscribes to this topic.
        // Frontend Logic: "If signal.sender === myUsername, ignore. Else, process."
        return signal;
    }
}