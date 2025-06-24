package com.serviceAgence.dto.auth;

import com.serviceAgence.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // en secondes
    
    // Informations utilisateur
    private String username;
    private String email;
    private String nom;
    private String prenom;
    private Set<UserRole> roles;
    private String idAgence;
    private String nomAgence;
    
    // Métadonnées de session
    private LocalDateTime loginTime;
    private Boolean firstLogin;
    private Boolean passwordExpired;
}
