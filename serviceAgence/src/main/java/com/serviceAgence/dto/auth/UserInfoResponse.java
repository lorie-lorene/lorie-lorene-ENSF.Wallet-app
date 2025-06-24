package com.serviceAgence.dto.auth;

import com.serviceAgence.enums.UserRole;
import com.serviceAgence.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO pour les informations utilisateur connecté
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String id;
    private String username;
    private String email;
    private String nom;
    private String prenom;
    private String idAgence;
    private String nomAgence;
    private Set<UserRole> roles;
    private UserStatus status;
    private LocalDateTime lastLogin;
    private String lastLoginIp;
    private Boolean firstLogin;
    private Boolean passwordExpired;
    private LocalDateTime passwordChangedAt;
}