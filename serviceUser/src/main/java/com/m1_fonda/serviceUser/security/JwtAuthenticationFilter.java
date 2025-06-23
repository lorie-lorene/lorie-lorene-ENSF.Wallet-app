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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
                
                // Extract user information from token
                String clientId = jwtService.extractClientId(token);
                String email = jwtService.extractSubject(token);
                String role = jwtService.extractRole(token);
                String status = jwtService.extractStatus(token);

                // Verify account is active for protected operations
                if (!"ACTIVE".equals(status) && !isStatusCheckEndpoint(requestPath)) {
                    handleInactiveAccount(response, status);
                    return;
                }

                // Create authentication token
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    clientId, // principal (client ID)
                    null,     // credentials
                    authorities
                );
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT authentication successful for client: {}", clientId);
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            handleExpiredToken(response);
        } catch (JwtException e) {
            log.warn("JWT token invalid: {}", e.getMessage());
            handleInvalidToken(response);
        } catch (Exception e) {
            log.error("JWT filter error: {}", e.getMessage(), e);
            handleGenericError(response);
        }
    }

    /**
     * Check if the endpoint is public (doesn't require authentication)
     */
    private boolean isPublicEndpoint(String requestPath) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(endpoint -> requestPath.startsWith(endpoint));
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
     * Handle missing authorization token
     */
    private void handleMissingToken(HttpServletResponse response) throws IOException {
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "MISSING_TOKEN", 
                         "Authorization token is required");
    }

    /**
     * Handle expired JWT token
     */
    private void handleExpiredToken(HttpServletResponse response) throws IOException {
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", 
                         "JWT token has expired. Please login again.");
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
