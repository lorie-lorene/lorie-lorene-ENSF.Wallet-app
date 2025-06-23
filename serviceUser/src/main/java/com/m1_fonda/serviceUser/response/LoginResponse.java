package com.m1_fonda.serviceUser.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 🔐 Login Response DTO
 * Contains authentication tokens and user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType; // "Bearer"
    private int expiresIn; // Token expiration time in seconds
    
    // User information
    private String clientId;
    private String email;
    private String status; // ACTIVE, PENDING, etc.
    private boolean isKycVerified;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLogin;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime issuedAt = LocalDateTime.now();
    
    // Additional metadata
    private String message = "Login successful";
}
