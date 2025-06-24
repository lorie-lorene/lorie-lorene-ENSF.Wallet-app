package com.serviceAgence.repository;

import com.serviceAgence.enums.UserRole;
import com.serviceAgence.enums.UserStatus;
import com.serviceAgence.model.AgenceUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des utilisateurs AgenceService
 */
@Repository
public interface AgenceUserRepository extends MongoRepository<AgenceUser, String> {
    
    /**
     * Recherche par username
     */
    Optional<AgenceUser> findByUsername(String username);
    
    /**
     * Recherche par email
     */
    Optional<AgenceUser> findByEmail(String email);
    
    /**
     * Recherche par refresh token
     */
    Optional<AgenceUser> findByRefreshToken(String refreshToken);
    
    /**
     * Vérification existence par username
     */
    boolean existsByUsername(String username);
    
    /**
     * Vérification existence par email
     */
    boolean existsByEmail(String email);
    
    /**
     * Recherche par rôle
     */
    List<AgenceUser> findByRolesContaining(UserRole role);
    
    /**
     * Recherche par agence
     */
    List<AgenceUser> findByIdAgence(String idAgence);
    
    /**
     * Recherche par statut
     */
    Page<AgenceUser> findByStatus(UserStatus status, Pageable pageable);
    
    /**
     * Utilisateurs avec tentatives de connexion échouées
     */
    @Query("{'failedLoginAttempts': {$gte: ?0}}")
    List<AgenceUser> findUsersWithFailedAttempts(int minFailedAttempts);
    
    /**
     * Utilisateurs connectés récemment
     */
    @Query("{'lastLogin': {$gte: ?0}}")
    List<AgenceUser> findRecentlyLoggedUsers(LocalDateTime since);
    
    /**
     * Tokens expirés à nettoyer
     */
    @Query("{'refreshTokenExpiry': {$lt: ?0}}")
    List<AgenceUser> findUsersWithExpiredTokens(LocalDateTime now);

    /**
     * Recherche avec filtres combinés
     */
    Page<AgenceUser> findByStatusAndUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            UserStatus status, String username, String email, Pageable pageable);

    /**
     * Recherche par terme (username ou email)
     */
    Page<AgenceUser> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, Pageable pageable);

    /**
     * Comptage par statut
     */
    long countByStatus(UserStatus status);
}