package com.serviceAgence.services;

import com.serviceAgence.dto.admin.*;
import com.serviceAgence.enums.UserRole;
import com.serviceAgence.enums.UserStatus;
import com.serviceAgence.exception.AuthenticationException;
import com.serviceAgence.model.AgenceUser;
import com.serviceAgence.repository.AgenceUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de gestion des utilisateurs par l'administrateur
 * Permet la création, modification, suspension des comptes utilisateurs
 */
@Service
@Transactional
@Slf4j
public class AdminUserManagementService {

    @Autowired
    private AgenceUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Création d'un nouvel utilisateur par l'admin
     */
    public CreateUserResponse createUser(CreateUserRequest request, String createdBy) {
        log.info("👤 Création utilisateur par admin: {} pour {}", createdBy, request.getUsername());

        // Validations
        validateCreateUserRequest(request);

        // Génération mot de passe temporaire
        String tempPassword = generateTemporaryPassword();

        // Création utilisateur
        AgenceUser user = new AgenceUser();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setRoles(request.getRoles());
        user.setStatus(UserStatus.ACTIVE);
        user.setIdAgence(request.getIdAgence());
        user.setNomAgence(request.getNomAgence());
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy(createdBy);
        user.setFirstLogin(true);
        user.setPasswordExpired(true); // Force changement au premier login

        AgenceUser savedUser = userRepository.save(user);

        log.info("✅ Utilisateur créé: {} avec rôles: {}", user.getUsername(), user.getRoles());

        return CreateUserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .temporaryPassword(tempPassword)
                .roles(savedUser.getRoles())
                .status(savedUser.getStatus())
                .message("Utilisateur créé avec succès. Mot de passe temporaire généré.")
                .build();
    }

    /**
     * Liste paginée des utilisateurs
     */
    public Page<UserSummaryDTO> getUsers(Pageable pageable, UserStatus status, String searchTerm) {
        log.info("📋 Récupération liste utilisateurs - Page: {}, Statut: {}, Recherche: {}", 
                pageable.getPageNumber(), status, searchTerm);

        Page<AgenceUser> users;

        if (status != null && searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Recherche avec statut et terme
            users = userRepository.findByStatusAndUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    status, searchTerm, searchTerm, pageable);
        } else if (status != null) {
            // Filtrage par statut uniquement
            users = userRepository.findByStatus(status, pageable);
        } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Recherche par terme uniquement
            users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    searchTerm, searchTerm, pageable);
        } else {
            // Tous les utilisateurs
            users = userRepository.findAll(pageable);
        }

        return users.map(this::convertToUserSummary);
    }

    /**
     * Détails complets d'un utilisateur
     */
    public UserDetailsDTO getUserDetails(String userId) {
        log.info("🔍 Récupération détails utilisateur: {}", userId);

        AgenceUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Utilisateur introuvable: " + userId));

        return convertToUserDetails(user);
    }

    /**
     * Mise à jour d'un utilisateur
     */
    public UserDetailsDTO updateUser(String userId, UpdateUserRequest request, String updatedBy) {
        log.info("📝 Mise à jour utilisateur: {} par {}", userId, updatedBy);

        AgenceUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Utilisateur introuvable: " + userId));

        // Vérifications d'unicité si changement
        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new AuthenticationException("Username déjà utilisé: " + request.getUsername());
        }

        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationException("Email déjà utilisé: " + request.getEmail());
        }

        // Mise à jour
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setRoles(request.getRoles());
        user.setIdAgence(request.getIdAgence());
        user.setNomAgence(request.getNomAgence());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(updatedBy);

        AgenceUser savedUser = userRepository.save(user);

        log.info("✅ Utilisateur mis à jour: {}", savedUser.getUsername());
        return convertToUserDetails(savedUser);
    }

    /**
     * Suspension d'un utilisateur
     */
    public void suspendUser(String userId, String reason, String suspendedBy) {
        log.info("🚫 Suspension utilisateur: {} par {} - Raison: {}", userId, suspendedBy, reason);

        AgenceUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Utilisateur introuvable: " + userId));

        // Ne pas permettre de suspendre le dernier admin actif
        if (user.getRoles().contains(UserRole.ADMIN)) {
            long activeAdmins = userRepository.findByRolesContaining(UserRole.ADMIN)
                    .stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .count();

            if (activeAdmins <= 1) {
                throw new AuthenticationException("Impossible de suspendre le dernier administrateur actif");
            }
        }

        user.setStatus(UserStatus.SUSPENDED);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(suspendedBy);
        // Note: Ajouter un champ 'suspensionReason' si nécessaire

        userRepository.save(user);
        log.info("✅ Utilisateur suspendu: {}", user.getUsername());
    }

    /**
     * Réactivation d'un utilisateur
     */
    public void reactivateUser(String userId, String reactivatedBy) {
        log.info("✅ Réactivation utilisateur: {} par {}", userId, reactivatedBy);

        AgenceUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Utilisateur introuvable: " + userId));

        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(reactivatedBy);

        userRepository.save(user);
        log.info("✅ Utilisateur réactivé: {}", user.getUsername());
    }

    /**
     * Réinitialisation du mot de passe
     */
    public ResetPasswordResponse resetUserPassword(String userId, String resetBy) {
        log.info("🔑 Réinitialisation mot de passe: {} par {}", userId, resetBy);

        AgenceUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Utilisateur introuvable: " + userId));

        String newTempPassword = generateTemporaryPassword();

        user.setPassword(passwordEncoder.encode(newTempPassword));
        user.setPasswordExpired(true);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(resetBy);

        userRepository.save(user);

        log.info("✅ Mot de passe réinitialisé: {}", user.getUsername());

        return ResetPasswordResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .temporaryPassword(newTempPassword)
                .message("Mot de passe réinitialisé. L'utilisateur devra le changer à la prochaine connexion.")
                .build();
    }

    /**
     * Suppression d'un utilisateur (soft delete)
     */
    public void deleteUser(String userId, String deletedBy) {
        log.info("🗑️ Suppression utilisateur: {} par {}", userId, deletedBy);

        AgenceUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Utilisateur introuvable: " + userId));

        // Protection contre la suppression du dernier admin
        if (user.getRoles().contains(UserRole.ADMIN)) {
            long activeAdmins = userRepository.findByRolesContaining(UserRole.ADMIN)
                    .stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .count();

            if (activeAdmins <= 1) {
                throw new AuthenticationException("Impossible de supprimer le dernier administrateur actif");
            }
        }

        user.setStatus(UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(deletedBy);

        userRepository.save(user);
        log.info("✅ Utilisateur supprimé (soft delete): {}", user.getUsername());
    }

    /**
     * Statistiques des utilisateurs
     */
    public UserStatisticsDTO getUserStatistics() {
        log.info("📊 Génération statistiques utilisateurs");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);
        long inactiveUsers = userRepository.countByStatus(UserStatus.INACTIVE);

        List<AgenceUser> recentUsers = userRepository.findRecentlyLoggedUsers(
                LocalDateTime.now().minusDays(7));

        return UserStatisticsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .suspendedUsers(suspendedUsers)
                .inactiveUsers(inactiveUsers)
                .recentlyActiveUsers(recentUsers.size())
                .adminUsers(userRepository.findByRolesContaining(UserRole.ADMIN).size())
                .agenceUsers(userRepository.findByRolesContaining(UserRole.AGENCE).size())
                .supervisorUsers(userRepository.findByRolesContaining(UserRole.SUPERVISOR).size())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ==========================================
    // MÉTHODES PRIVÉES
    // ==========================================

    private void validateCreateUserRequest(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthenticationException("Username déjà utilisé: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationException("Email déjà utilisé: " + request.getEmail());
        }

        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            throw new AuthenticationException("Au moins un rôle doit être spécifié");
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    private UserSummaryDTO convertToUserSummary(AgenceUser user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .roles(user.getRoles())
                .status(user.getStatus())
                .idAgence(user.getIdAgence())
                .nomAgence(user.getNomAgence())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserDetailsDTO convertToUserDetails(AgenceUser user) {
        return UserDetailsDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .roles(user.getRoles())
                .status(user.getStatus())
                .idAgence(user.getIdAgence())
                .nomAgence(user.getNomAgence())
                .lastLogin(user.getLastLogin())
                .lastLoginIp(user.getLastLoginIp())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .accountLockedUntil(user.getAccountLockedUntil())
                .firstLogin(user.getFirstLogin())
                .passwordExpired(user.getPasswordExpired())
                .passwordChangedAt(user.getPasswordChangedAt())
                .createdAt(user.getCreatedAt())
                .createdBy(user.getCreatedBy())
                .updatedAt(user.getUpdatedAt())
                .updatedBy(user.getUpdatedBy())
                .build();
    }
}
