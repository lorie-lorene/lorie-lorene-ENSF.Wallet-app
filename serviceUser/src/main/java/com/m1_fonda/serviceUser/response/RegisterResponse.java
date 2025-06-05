package com.m1_fonda.serviceUser.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {
    private String status;
    private String message;
    private String requestId;
    private LocalDateTime timestamp;
    
    public RegisterResponse(String status, String message) {
        this.status = status;
        this.message = message;
        this.requestId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }
}