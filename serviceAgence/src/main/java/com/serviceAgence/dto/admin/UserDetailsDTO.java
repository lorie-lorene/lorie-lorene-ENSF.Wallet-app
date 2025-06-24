package com.serviceAgence.dto.admin;

import com.serviceAgence.enums.UserRole;
import com.serviceAgence.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDTO {
    private String id;
    private String username;
    private String email;
    private String nom;
    private String prenom;
    private Set<UserRole> roles;
    private UserStatus status;
    private String idAgence;
    private String nomAgence;
    
    // Informations de sécurité
    private LocalDateTime lastLogin;
    private String lastLoginIp;
    private Integer failedLoginAttempts;
    private LocalDateTime accountLockedUntil;
    private Boolean firstLogin;
    private Boolean passwordExpired;
    private LocalDateTime passwordChangedAt;
    
    // Audit
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}