package com.serviceAgence.model;

import com.serviceAgence.enums.UserRole;
import com.serviceAgence.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Entité utilisateur pour l'authentification AgenceService
 * Stocke les informations des administrateurs, superviseurs et employés d'agence
 */
@Document(collection = "agence_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgenceUser {
    
    @Id
    private String id;
    
    @NotBlank(message = "Username obligatoire")
    @Indexed(unique = true)
    private String username;
    
    @NotBlank(message = "Email obligatoire")
    @Email(message = "Format email invalide")
    @Indexed(unique = true)
    private String email;
    
    @NotBlank(message = "Mot de passe obligatoire")
    private String password; // BCrypt hashé
    
    @NotBlank(message = "Nom obligatoire")
    private String nom;
    
    @NotBlank(message = "Prénom obligatoire")
    private String prenom;
    
    // Informations agence (pour employés d'agence)
    private String idAgence;
    private String nomAgence;
    
    // Rôles et permissions
    @NotNull
    private Set<UserRole> roles;
    
    @NotNull
    private UserStatus status = UserStatus.ACTIVE;
    
    // Sécurité
    private LocalDateTime lastLogin;
    private String lastLoginIp;
    private Integer failedLoginAttempts = 0;
    private LocalDateTime accountLockedUntil;
    
    // Gestion des tokens
    private String refreshToken;
    private LocalDateTime refreshTokenExpiry;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Métadonnées
    private Boolean firstLogin = true;
    private Boolean passwordExpired = false;
    private LocalDateTime passwordChangedAt;
    
    /**
     * Vérification si le compte est verrouillé
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && LocalDateTime.now().isBefore(accountLockedUntil);
    }
    
    /**
     * Vérification si le refresh token est valide
     */
    public boolean isRefreshTokenValid() {
        return refreshToken != null && 
               refreshTokenExpiry != null && 
               LocalDateTime.now().isBefore(refreshTokenExpiry);
    }
    
    /**
     * Marque une tentative de connexion échouée
     */
    public void markFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            // Verrouillage du compte pour 30 minutes
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }
    
    /**
     * Réinitialise les tentatives de connexion
     */
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }
    
    /**
     * Mise à jour des informations de dernière connexion
     */
    public void updateLastLogin(String ipAddress) {
        this.lastLogin = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.firstLogin = false;
        resetFailedAttempts();
    }
}