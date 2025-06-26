package com.serviceAgence.controller;

import com.serviceAgence.dto.admin.*;
import com.serviceAgence.enums.UserStatus;
import com.serviceAgence.services.AdminUserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur de gestion des utilisateurs par l'administrateur
 * Permet la création, modification, suspension et gestion des comptes utilisateurs
 */
@RestController
@RequestMapping("/api/v1/agence/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@Slf4j
@Tag(name = "Admin User Management", description = "Gestion des utilisateurs par l'administrateur")
public class AdminController {

    @Autowired
    private AdminUserManagementService adminUserService;

    /**
     * Création d'un nouvel utilisateur
     */
    @PostMapping
    @Operation(summary = "Créer un nouvel utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "409", description = "Username ou email déjà utilisé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<CreateUserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {

        try {
            String createdBy = authentication.getName();
            CreateUserResponse response = adminUserService.createUser(request, createdBy);

            log.info("✅ Utilisateur créé par admin: {} -> {}", createdBy, request.getUsername());
            return ResponseEntity.status(201).body(response);

        } catch (Exception e) {
            log.error("❌ Erreur création utilisateur: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Liste paginée des utilisateurs avec filtres
     */
    @GetMapping
    @Operation(summary = "Lister les utilisateurs avec filtres")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste récupérée"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<Page<UserSummaryDTO>> getUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search) {

        try {
            Page<UserSummaryDTO> users = adminUserService.getUsers(pageable, status, search);

            log.info("📋 Liste utilisateurs récupérée: {} utilisateurs", users.getTotalElements());
            return ResponseEntity.ok(users);

        } catch (Exception e) {
            log.error("❌ Erreur récupération utilisateurs: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Détails complets d'un utilisateur
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Détails d'un utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Détails récupérés"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<UserDetailsDTO> getUserDetails(@PathVariable String userId) {

        try {
            UserDetailsDTO userDetails = adminUserService.getUserDetails(userId);

            log.info("🔍 Détails utilisateur récupérés: {}", userId);
            return ResponseEntity.ok(userDetails);

        } catch (Exception e) {
            log.error("❌ Erreur récupération détails utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Mise à jour d'un utilisateur
     */
    @PutMapping("/{userId}")
    @Operation(summary = "Mettre à jour un utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Utilisateur mis à jour"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
        @ApiResponse(responseCode = "409", description = "Username ou email déjà utilisé")
    })
    public ResponseEntity<UserDetailsDTO> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        try {
            String updatedBy = authentication.getName();
            UserDetailsDTO updatedUser = adminUserService.updateUser(userId, request, updatedBy);

            log.info("📝 Utilisateur mis à jour par admin: {} -> {}", updatedBy, userId);
            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            log.error("❌ Erreur mise à jour utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Suspension d'un utilisateur
     */
    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspendre un utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Utilisateur suspendu"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
        @ApiResponse(responseCode = "400", description = "Impossible de suspendre ce compte")
    })
    public ResponseEntity<Map<String, String>> suspendUser(
            @PathVariable String userId,
            @RequestParam String reason,
            Authentication authentication) {

        try {
            String suspendedBy = authentication.getName();
            adminUserService.suspendUser(userId, reason, suspendedBy);

            Map<String, String> response = Map.of(
                "status", "SUCCESS",
                "message", "Utilisateur suspendu avec succès",
                "userId", userId,
                "reason", reason
            );

            log.info("🚫 Utilisateur suspendu par admin: {} -> {}", suspendedBy, userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur suspension utilisateur {}: {}", userId, e.getMessage());
            
            Map<String, String> response = Map.of(
                "status", "ERROR",
                "message", e.getMessage(),
                "userId", userId
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Réactivation d'un utilisateur
     */
    @PostMapping("/{userId}/reactivate")
    @Operation(summary = "Réactiver un utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Utilisateur réactivé"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    public ResponseEntity<Map<String, String>> reactivateUser(
            @PathVariable String userId,
            Authentication authentication) {

        try {
            String reactivatedBy = authentication.getName();
            adminUserService.reactivateUser(userId, reactivatedBy);

            Map<String, String> response = Map.of(
                "status", "SUCCESS",
                "message", "Utilisateur réactivé avec succès",
                "userId", userId
            );

            log.info("✅ Utilisateur réactivé par admin: {} -> {}", reactivatedBy, userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur réactivation utilisateur {}: {}", userId, e.getMessage());
            
            Map<String, String> response = Map.of(
                "status", "ERROR",
                "message", e.getMessage(),
                "userId", userId
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Réinitialisation du mot de passe
     */
    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe d'un utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    public ResponseEntity<ResetPasswordResponse> resetUserPassword(
            @PathVariable String userId,
            Authentication authentication) {

        try {
            String resetBy = authentication.getName();
            ResetPasswordResponse response = adminUserService.resetUserPassword(userId, resetBy);

            log.info("🔑 Mot de passe réinitialisé par admin: {} -> {}", resetBy, userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur réinitialisation mot de passe {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Suppression d'un utilisateur (soft delete)
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Supprimer un utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Utilisateur supprimé"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
        @ApiResponse(responseCode = "400", description = "Impossible de supprimer ce compte")
    })
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable String userId,
            Authentication authentication) {

        try {
            String deletedBy = authentication.getName();
            adminUserService.deleteUser(userId, deletedBy);

            Map<String, String> response = Map.of(
                "status", "SUCCESS",
                "message", "Utilisateur supprimé avec succès",
                "userId", userId
            );

            log.info("🗑️ Utilisateur supprimé par admin: {} -> {}", deletedBy, userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur suppression utilisateur {}: {}", userId, e.getMessage());
            
            Map<String, String> response = Map.of(
                "status", "ERROR",
                "message", e.getMessage(),
                "userId", userId
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Statistiques des utilisateurs
     */
    @GetMapping("/statistics")
    @Operation(summary = "Statistiques des utilisateurs")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistiques récupérées"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<UserStatisticsDTO> getUserStatistics() {

        try {
            UserStatisticsDTO statistics = adminUserService.getUserStatistics();

            log.info("📊 Statistiques utilisateurs générées");
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("❌ Erreur génération statistiques: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export des utilisateurs (CSV - optionnel)
     */
    @GetMapping("/export")
    @Operation(summary = "Exporter la liste des utilisateurs")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Export généré"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<Map<String, String>> exportUsers(
            @RequestParam(required = false) UserStatus status,
            Authentication authentication) {

        try {
            // Cette fonctionnalité peut être implémentée plus tard
            Map<String, String> response = Map.of(
                "status", "INFO",
                "message", "Fonctionnalité d'export à implémenter",
                "requestedBy", authentication.getName()
            );

            log.info("📁 Demande d'export par: {}", authentication.getName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur export: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}