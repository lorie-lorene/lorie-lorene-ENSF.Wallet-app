package com.m1_fonda.serviceUser.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 📊 Authentication Status Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthStatusResponse {
    
    private boolean authenticated;
    private String clientId;
    private String email;
    private String status;
    private boolean isKycVerified;
    private boolean tokenExpired;
    private long tokenValidityRemaining; // in milliseconds
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLogin;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime checkedAt = LocalDateTime.now();
}