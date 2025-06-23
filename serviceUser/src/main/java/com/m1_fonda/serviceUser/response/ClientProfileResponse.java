package com.m1_fonda.serviceUser.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 👤 Client Profile Response DTO
 * Contains client profile information for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfileResponse {
    private String idClient;
    private String nom;
    private String prenom;
    private String email;
    private String numero;
    private String status;
    private boolean isKycVerified;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLogin;
    
    private String agencyId;
    private String agencyName;
    
    // Additional metadata
    private boolean emailVerified = true;
    private boolean phoneVerified = true;
}