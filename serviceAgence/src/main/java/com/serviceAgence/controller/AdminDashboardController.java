package com.serviceAgence.controller;

import com.serviceAgence.dto.admin.UserStatisticsDTO;
import com.serviceAgence.services.AdminUserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contrôleur du tableau de bord administrateur
 * Fournit des vues d'ensemble et des métriques pour l'administration
 */
@RestController
@RequestMapping("/api/v1/agence/admin/dashboard")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
@Slf4j
@Tag(name = "Admin Dashboard", description = "Tableau de bord administrateur")
public class AdminDashboardController {

    @Autowired
    private AdminUserManagementService adminUserService;

    /**
     * Vue d'ensemble du tableau de bord
     */
    @GetMapping
    @Operation(summary = "Vue d'ensemble du dashboard admin")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard récupéré"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<Map<String, Object>> getDashboard() {

        try {
            UserStatisticsDTO userStats = adminUserService.getUserStatistics();

            Map<String, Object> dashboard = Map.of(
                "systemInfo", Map.of(
                    "serviceName", "AgenceService",
                    "version", "1.0.0",
                    "uptime", "Calcul à implémenter",
                    "environment", "Development" // Depuis configuration
                ),
                "userStatistics", userStats,
                "quickActions", Map.of(
                    "createUser", "/api/v1/agence/admin/users",
                    "viewUsers", "/api/v1/agence/admin/users",
                    "userStats", "/api/v1/agence/admin/users/statistics"
                ),
                "generatedAt", LocalDateTime.now()
            );

            log.info("📊 Dashboard admin généré");
            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("❌ Erreur génération dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Santé du système
     */
    @GetMapping("/health")
    @Operation(summary = "État de santé du système")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {

        try {
            // Vérifications basiques du système
            Map<String, Object> health = Map.of(
                "status", "UP",
                "database", "UP", // À vérifier avec un ping DB
                "messaging", "UP", // À vérifier avec RabbitMQ
                "dependencies", Map.of(
                    "mongodb", "UP",
                    "rabbitmq", "UP"
                ),
                "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("❌ Erreur vérification santé système: {}", e.getMessage());
            
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Activité récente du système
     */
    @GetMapping("/recent-activity")
    @Operation(summary = "Activité récente du système")
    public ResponseEntity<Map<String, Object>> getRecentActivity() {

        try {
            // Cette fonctionnalité nécessiterait un service d'audit
            Map<String, Object> activity = Map.of(
                "recentLogins", "À implémenter avec service d'audit",
                "recentUserCreations", "À implémenter",
                "systemEvents", "À implémenter",
                "message", "Fonctionnalité en cours de développement"
            );

            return ResponseEntity.ok(activity);

        } catch (Exception e) {
            log.error("❌ Erreur récupération activité: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}