package com.serviceAgence.dto.admin;

import com.serviceAgence.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO pour la création d'un utilisateur par l'admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    
    @NotBlank(message = "Username obligatoire")
    @Size(min = 3, max = 50, message = "Username doit contenir entre 3 et 50 caractères")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username ne peut contenir que lettres, chiffres, _, . et -")
    private String username;
    
    @NotBlank(message = "Email obligatoire")
    @Email(message = "Format email invalide")
    private String email;
    
    @NotBlank(message = "Nom obligatoire")
    @Size(min = 2, max = 100, message = "Nom doit contenir entre 2 et 100 caractères")
    private String nom;
    
    @NotBlank(message = "Prénom obligatoire")
    @Size(min = 2, max = 100, message = "Prénom doit contenir entre 2 et 100 caractères")
    private String prenom;
    
    @NotEmpty(message = "Au moins un rôle doit être spécifié")
    private Set<UserRole> roles;
    
    // Optionnel pour les employés d'agence
    private String idAgence;
    private String nomAgence;
}