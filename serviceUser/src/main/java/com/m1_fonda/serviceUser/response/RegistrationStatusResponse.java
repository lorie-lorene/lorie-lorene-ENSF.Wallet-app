package com.m1_fonda.serviceUser.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 📋 Registration Status Response DTO
 * Response for registration status checks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationStatusResponse {
    private String status;
    private String message;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String rejectionReason;
    private boolean canReapply;
    private String nextAction;
    
    // Constructors for backward compatibility
    public RegistrationStatusResponse(String status, String message, LocalDateTime createdAt) {
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
    }
}