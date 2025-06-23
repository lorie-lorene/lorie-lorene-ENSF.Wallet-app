package com.m1_fonda.serviceUser.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 📝 Register Response DTO
 * Response for user registration requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String status; // PENDING, ACCEPTED, REJECTED
    private String message;
    private String requestId; // Client ID or tracking ID
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String nextSteps;
    private int estimatedProcessingDays;
    
    // Additional information
    private boolean documentsRequired;
    private String agencyContact;

     public RegisterResponse(String status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public RegisterResponse(String status, String message, String requestId) {
        this.status = status;
        this.message = message;
        this.requestId = requestId;
        this.timestamp = LocalDateTime.now();
    }
}