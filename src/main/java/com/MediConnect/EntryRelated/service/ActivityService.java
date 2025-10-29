package com.MediConnect.EntryRelated.service;

import com.MediConnect.EntryRelated.dto.AccountActivityDTO;
import com.MediConnect.EntryRelated.dto.LoginSessionDTO;
import com.MediConnect.EntryRelated.entities.AccountActivity;
import com.MediConnect.EntryRelated.entities.LoginSession;
import com.MediConnect.EntryRelated.entities.Users;
import com.MediConnect.EntryRelated.repository.AccountActivityRepository;
import com.MediConnect.EntryRelated.repository.LoginSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {
    
    private final LoginSessionRepository loginSessionRepository;
    private final AccountActivityRepository accountActivityRepository;
    
    /**
     * Create a new login session
     */
    @Transactional
    public LoginSession createLoginSession(Users user, String sessionToken, HttpServletRequest request) {
        LoginSession session = new LoginSession();
        session.setUser(user);
        session.setSessionToken(sessionToken);
        session.setIpAddress(getClientIpAddress(request));
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setLocation(detectLocation(request)); // You can implement IP geolocation
        session.setDevice(detectDevice(request));
        session.setBrowser(detectBrowser(request));
        session.setLoginTime(LocalDateTime.now());
        session.setLastActivityTime(LocalDateTime.now());
        
        LoginSession savedSession = loginSessionRepository.save(session);
        
        // Log the login activity
        logAccountActivity(user, AccountActivity.ActivityType.LOGIN, 
            "Successful login from " + session.getBrowser() + " on " + session.getDevice(), 
            request);
        
        log.info("Created login session for user {} with session token {}", user.getUsername(), sessionToken);
        return savedSession;
    }
    
    /**
     * Logout a user from a specific session
     */
    @Transactional
    public void logoutSession(String sessionToken) {
        loginSessionRepository.findBySessionToken(sessionToken)
            .ifPresent(session -> {
                session.logout();
                loginSessionRepository.save(session);
                
                // Log the logout activity
                logAccountActivity(session.getUser(), AccountActivity.ActivityType.LOGOUT,
                    "Logged out from " + session.getBrowser() + " on " + session.getDevice(),
                    null);
                
                log.info("Logged out session {} for user {}", sessionToken, session.getUser().getUsername());
            });
    }
    
    /**
     * Log account activity
     */
    @Transactional
    public void logAccountActivity(Users user, AccountActivity.ActivityType type, String description, HttpServletRequest request) {
        AccountActivity activity = new AccountActivity();
        activity.setUser(user);
        activity.setType(type);
        activity.setDescription(description);
        
        if (request != null) {
            activity.setIpAddress(getClientIpAddress(request));
            activity.setUserAgent(request.getHeader("User-Agent"));
            activity.setLocation(detectLocation(request));
            activity.setDevice(detectDevice(request));
        }
        
        accountActivityRepository.save(activity);
        log.debug("Logged activity {} for user {}", type, user.getUsername());
    }
    
    /**
     * Get login sessions for a user
     */
    public List<LoginSessionDTO> getLoginSessions(Users user) {
        List<LoginSession> sessions = loginSessionRepository.findByUserOrderByLoginTimeDesc(user);
        return sessions.stream()
            .map(this::convertToLoginSessionDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get recent account activities for a user
     */
    public List<AccountActivityDTO> getAccountActivities(Users user, int limit) {
        List<AccountActivity> activities = accountActivityRepository.findRecentActivitiesByUser(user, limit);
        return activities.stream()
            .map(this::convertToAccountActivityDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Update session activity
     */
    @Transactional
    public void updateSessionActivity(String sessionToken) {
        loginSessionRepository.findBySessionToken(sessionToken)
            .ifPresent(session -> {
                session.updateActivity();
                loginSessionRepository.save(session);
            });
    }
    
    /**
     * Cleanup old inactive sessions
     */
    @Transactional
    public void cleanupOldSessions(Users user, int daysToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
        loginSessionRepository.deleteByUserAndIsActiveFalseAndLogoutTimeBefore(user, cutoffTime);
        
        // Also cleanup old account activities
        accountActivityRepository.deleteByUserAndTimestampBefore(user, cutoffTime);
        
        log.info("Cleaned up old sessions and activities for user {}", user.getUsername());
    }
    
    // Helper methods
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private String detectLocation(HttpServletRequest request) {
        // TODO: Implement IP geolocation service
        // For now, return a placeholder
        return "Unknown Location";
    }
    
    private String detectDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            if (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone")) {
                return "Mobile";
            } else if (userAgent.contains("Tablet") || userAgent.contains("iPad")) {
                return "Tablet";
            } else {
                return "Desktop";
            }
        }
        return "Unknown Device";
    }
    
    private String detectBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            if (userAgent.contains("Chrome")) return "Chrome";
            if (userAgent.contains("Firefox")) return "Firefox";
            if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) return "Safari";
            if (userAgent.contains("Edge")) return "Edge";
            if (userAgent.contains("Opera")) return "Opera";
        }
        return "Unknown Browser";
    }
    
    private LoginSessionDTO convertToLoginSessionDTO(LoginSession session) {
        LoginSessionDTO dto = new LoginSessionDTO();
        dto.setId(session.getId());
        dto.setSessionToken(session.getSessionToken());
        dto.setIpAddress(session.getIpAddress());
        dto.setUserAgent(session.getUserAgent());
        dto.setLocation(session.getLocation());
        dto.setDevice(session.getDevice());
        dto.setBrowser(session.getBrowser());
        dto.setLoginTime(session.getLoginTime());
        dto.setLogoutTime(session.getLogoutTime());
        dto.setIsActive(session.getIsActive());
        dto.setLastActivityTime(session.getLastActivityTime());
        dto.setIsCurrentSession(false); // This will be set by the controller
        return dto;
    }
    
    private AccountActivityDTO convertToAccountActivityDTO(AccountActivity activity) {
        AccountActivityDTO dto = new AccountActivityDTO();
        dto.setId(activity.getId());
        dto.setType(activity.getType());
        dto.setDescription(activity.getDescription());
        dto.setIpAddress(activity.getIpAddress());
        dto.setLocation(activity.getLocation());
        dto.setDevice(activity.getDevice());
        dto.setUserAgent(activity.getUserAgent());
        dto.setTimestamp(activity.getTimestamp());
        dto.setAdditionalData(activity.getAdditionalData());
        return dto;
    }
}
