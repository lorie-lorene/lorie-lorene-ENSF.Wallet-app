package com.serviceAgence.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les demandes de connexion
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "Username obligatoire")
    private String username;
    
    @NotBlank(message = "Mot de passe obligatoire")
    private String password;
    
    private Boolean rememberMe = false;
}