package com.serviceAgence.controller;

import com.serviceAgence.dto.auth.*;
import com.serviceAgence.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Contrôleur d'authentification pour AgenceService
 * Gère login, logout, refresh tokens et gestion des mots de passe
 */
@RestController
@RequestMapping("/api/v1/agence/auth")
@Validated
@Slf4j
@Tag(name = "Authentication", description = "API d'authentification AgenceService")
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Connexion utilisateur
     */
    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connexion réussie"),
        @ApiResponse(responseCode = "401", description = "Identifiants invalides"),
        @ApiResponse(responseCode = "423", description = "Compte verrouillé"),
        @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            LoginResponse response = authenticationService.login(request, ipAddress);

            log.info("✅ Connexion réussie: {} depuis {}", request.getUsername(), ipAddress);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Échec connexion: {} - {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body(null);
        }
    }

    /**
     * Renouvellement du token d'accès
     */
    @PostMapping("/refresh")
    @Operation(summary = "Renouveler le token d'accès")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token renouvelé"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalide"),
        @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        try {
            LoginResponse response = authenticationService.refreshToken(request);
            
            log.info("✅ Token renouvelé avec succès");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Échec renouvellement token: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Déconnexion utilisateur
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Déconnexion utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {

        try {
            String username = authentication.getName();
            authenticationService.logout(username);

            Map<String, String> response = Map.of(
                "status", "SUCCESS",
                "message", "Déconnexion réussie",
                "username", username
            );

            log.info("👋 Déconnexion: {}", username);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur déconnexion: {}", e.getMessage());
            
            Map<String, String> response = Map.of(
                "status", "ERROR",
                "message", "Erreur lors de la déconnexion"
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Changement de mot de passe
     */
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Changer le mot de passe")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mot de passe changé"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Mot de passe actuel incorrect")
    })
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            authenticationService.changePassword(username, request);

            Map<String, String> response = Map.of(
                "status", "SUCCESS",
                "message", "Mot de passe changé avec succès",
                "username", username
            );

            log.info("🔑 Mot de passe changé: {}", username);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur changement mot de passe: {}", e.getMessage());
            
            Map<String, String> response = Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Informations utilisateur connecté
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Informations utilisateur connecté")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Informations récupérées"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<UserInfoResponse> getCurrentUser(Authentication authentication) {

        try {
            String username = authentication.getName();
            UserInfoResponse response = authenticationService.getCurrentUserInfo(username);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur récupération infos utilisateur: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Vérification validité du token
     */
    @PostMapping("/validate-token")
    @Operation(summary = "Valider un token JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token valide"),
        @ApiResponse(responseCode = "401", description = "Token invalide")
    })
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestBody Map<String, String> request) {

        try {
            String token = request.get("token");
            boolean isValid = authenticationService.validateToken(token);

            Map<String, Object> response = Map.of(
                "valid", isValid,
                "message", isValid ? "Token valide" : "Token invalide"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "valid", false,
                "message", "Erreur validation token"
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Extraction de l'adresse IP du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
