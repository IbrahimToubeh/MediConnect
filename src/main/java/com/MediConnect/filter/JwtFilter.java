package com.MediConnect.filter;

import com.MediConnect.EntryRelated.service.MyUserDetailsService;
import com.MediConnect.config.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JWTService jwtService;
    private final ApplicationContext applicationContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request
            , HttpServletResponse response
            , FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        
        // Only process if Authorization header exists and starts with "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            
            // Skip if token is empty or just whitespace
            if (token == null || token.trim().isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Try to extract username from token
            // If token is malformed, expired, or invalid, catch the exception and continue
            // This allows requests without valid tokens to proceed (Spring Security will handle authorization)
            try {
                username = jwtService.extractUserName(token);
            } catch (Exception e) {
                // Token is invalid, malformed, or expired
                // Only log errors, not successful authentications
                System.err.println("JWT Filter: Token validation failed: " + e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }
        }
        
        // If we have a valid username and no existing authentication, proceed with authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = applicationContext.getBean(MyUserDetailsService.class).loadUserByUsername(username);
                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // If user lookup or token validation fails, continue without authentication
                // Spring Security will handle authorization based on endpoint configuration
                System.err.println("JWT Filter: Failed to authenticate user: " + e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
