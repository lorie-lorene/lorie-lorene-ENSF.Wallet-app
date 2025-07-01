package com.m1_fonda.serviceUser.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.m1_fonda.serviceUser.service.JwtService;
import com.m1_fonda.serviceUser.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🔐 JWT Authentication Filter
 * Intercepts requests and validates JWT tokens for authentication
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
        "/api/v1/users/register",
        "/api/v1/users/login", 
        "/api/v1/users/registration-status",
        "/api/v1/users/password-reset/request",
        "/api/v1/users/refresh-token",
        "/swagger-ui",
        "/v3/api-docs",
        "/actuator/health"
    );

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

    try {
        String requestPath = request.getRequestURI();
        
        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = jwtService.extractTokenFromHeader(authHeader);

        if (token == null) {
            handleMissingToken(response);
            return;
        }

        // Validate and process token
        if (jwtService.isTokenValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // Determine token type and set authentication accordingly
            if (jwtService.isAgenceServiceToken(token)) {
                // Handle AgenceService admin token
                handleAgenceServiceToken(token, request);
                
            } else if (jwtService.isUserServiceToken(token)) {
                // Handle UserService client token
                handleUserServiceToken(token, request);
                
            } else {
                log.warn("Unknown token type for path: {}", requestPath);
                handleInvalidToken(response, "Unknown token type");
                return;
            }
        } else if (token != null) {
            // Token exists but is invalid
            handleInvalidToken(response, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);

    } catch (ExpiredJwtException e) {
        log.warn("JWT token expired: {}", e.getMessage());
        handleExpiredToken(response);
    } catch (JwtException e) {
        log.error("JWT error: {}", e.getMessage());
        handleInvalidToken(response, "JWT processing error");
    } catch (Exception e) {
        log.error("Authentication filter error: {}", e.getMessage(), e);
        handleAuthenticationError(response);
    }
}

// @Override
// protected void doFilterInternal(
//         HttpServletRequest request,
//         HttpServletResponse response,
//         FilterChain filterChain) throws ServletException, IOException {

//     try {
//         String requestPath = request.getRequestURI();
        
//         // Skip JWT validation for public endpoints
//         if (isPublicEndpoint(requestPath)) {
//             filterChain.doFilter(request, response);
//             return;
//         }

//         // Extract token from Authorization header
//         String authHeader = request.getHeader("Authorization");

//         // **TEST BYPASS**: Accept any Bearer token
//         if (authHeader != null && authHeader.startsWith("Bearer ")) {
//             List<SimpleGrantedAuthority> authorities = Collections.singletonList(
//                 new SimpleGrantedAuthority("ROLE_CLIENT")
//             );

//             UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
//                 "test-client-id", null, authorities
//             );
            
//             authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//             SecurityContextHolder.getContext().setAuthentication(authToken);
            
//             log.debug("🔓 TEST MODE: Bypassing JWT validation");
//         }

//         filterChain.doFilter(request, response);

//     } catch (Exception e) {
//         log.error("JWT filter error: {}", e.getMessage(), e);
//         filterChain.doFilter(request, response);
//     }
// }

/**
 * Handle AgenceService admin tokens
 */
private void handleAgenceServiceToken(String token, HttpServletRequest request) {
    try {
        String username = jwtService.extractSubject(token);
        Collection<? extends GrantedAuthority> authorities = jwtService.extractAuthorities(token);
        
        if (username != null && !authorities.isEmpty()) {
            // Create authentication for admin user
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("🔐 AgenceService admin authenticated: {} with authorities: {}", 
                    username, authorities);
        }
        
    } catch (Exception e) {
        log.error("Error processing AgenceService token: {}", e.getMessage());
        throw new JwtException("AgenceService token processing failed", e);
    }
}

/**
 * Handle UserService client tokens
 */
private void handleUserServiceToken(String token, HttpServletRequest request) {
    try {
        // Extract user information from token
        String clientId = jwtService.extractClientId(token);
        String email = jwtService.extractSubject(token);
        String role = jwtService.extractRole(token);
        String status = jwtService.extractStatus(token);

        if (clientId != null && email != null) {
            // Check if client status allows access
            if (!"ACTIVE".equals(status)) {
                log.warn("Client {} has non-active status: {}", clientId, status);
                // For non-critical operations, still allow but log
            }

            // Create authorities based on role and status
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            
            // Add status-based authority
            if (status != null) {
                authorities.add(new SimpleGrantedAuthority("STATUS_" + status));
            }

            // Create authentication for client user
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            
            // Add client-specific details
            Map<String, Object> details = new HashMap<>();
            details.put("clientId", clientId);
            details.put("status", status);
            details.put("role", role);
            details.put("requestDetails", new WebAuthenticationDetailsSource().buildDetails(request));
            authentication.setDetails(details);
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("🔐 UserService client authenticated: {} (clientId: {}, status: {})", 
                    email, clientId, status);
        }
        
    } catch (Exception e) {
        log.error("Error processing UserService token: {}", e.getMessage());
        throw new JwtException("UserService token processing failed", e);
    }
}

/**
 * Check if endpoint is public (doesn't require authentication)
 */
private boolean isPublicEndpoint(String path) {
    return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
}

/**
 * Handle missing token
 */
private void handleMissingToken(HttpServletResponse response) throws IOException {
    log.debug("No JWT token found in request");
    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
            "MISSING_TOKEN", "Authentication token is required");
}

/**
 * Handle invalid token
 */
private void handleInvalidToken(HttpServletResponse response, String message) throws IOException {
    log.warn("Invalid JWT token: {}", message);
    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
            "INVALID_TOKEN", message);
}

/**
 * Handle expired token
 */
private void handleExpiredToken(HttpServletResponse response) throws IOException {
    log.warn("JWT token has expired");
    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
            "EXPIRED_TOKEN", "Authentication token has expired");
}

/**
 * Handle general authentication errors
 */
private void handleAuthenticationError(HttpServletResponse response) throws IOException {
    log.error("Authentication processing error");
    sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, 
            "AUTH_ERROR", "Authentication processing failed");
}
    /**
     * Check if the endpoint is for status checking (allows non-active accounts)
     */
    private boolean isStatusCheckEndpoint(String requestPath) {
        return requestPath.contains("/auth-status") || 
               requestPath.contains("/profile") ||
               requestPath.contains("/logout");
    }

    /**
     * Handle invalid JWT token
     */
    private void handleInvalidToken(HttpServletResponse response) throws IOException {
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", 
                         "JWT token is invalid");
    }

    /**
     * Handle inactive account
     */
    private void handleInactiveAccount(HttpServletResponse response, String status) throws IOException {
        String message = switch (status) {
            case "PENDING" -> "Account pending approval. Please wait for verification.";
            case "REJECTED" -> "Account rejected. Please contact support.";
            case "BLOCKED" -> "Account blocked. Please contact customer support.";
            case "SUSPENDED" -> "Account temporarily suspended. Please contact support.";
            default -> "Account not active. Please contact support.";
        };
        
        sendErrorResponse(response, HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", message);
    }

    /**
     * Handle generic authentication error
     */
    private void handleGenericError(HttpServletResponse response) throws IOException {
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR", 
                         "Authentication failed");
    }

    /**
     * Send error response as JSON
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, 
                                 String error, String message) throws IOException {
        
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .build();

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
