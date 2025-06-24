package com.serviceAgence.dto.admin;

import com.serviceAgence.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    
    @NotBlank(message = "Username obligatoire")
    @Size(min = 3, max = 50)
    private String username;
    
    @NotBlank(message = "Email obligatoire")
    @Email(message = "Format email invalide")
    private String email;
    
    @NotBlank(message = "Nom obligatoire")
    private String nom;
    
    @NotBlank(message = "Prénom obligatoire")
    private String prenom;
    
    @NotEmpty(message = "Au moins un rôle doit être spécifié")
    private Set<UserRole> roles;
    
    private String idAgence;
    private String nomAgence;
}