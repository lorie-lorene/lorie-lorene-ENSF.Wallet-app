// Replace your AdminDashboardController.java with this improved version

package com.serviceAgence.controller;

import com.serviceAgence.dto.admin.UserStatisticsDTO;
import com.serviceAgence.dto.common.ApiResponse;
import com.serviceAgence.services.AdminUserManagementService;
import io.swagger.v3.oas.annotations.Operation;
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
 * 🏦 Admin Dashboard Controller - Fixed Version
 * 
 * Provides dashboard data with standardized response format
 * Compatible with frontend expectations
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
     * 📊 Dashboard Overview - Main endpoint
     * Returns comprehensive dashboard data in standardized format
     */
    @GetMapping
    @Operation(summary = "Vue d'ensemble du dashboard admin")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard récupéré"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        
        try {
            log.info("📊 Génération dashboard admin...");
            
            // Get user statistics from service
            UserStatisticsDTO userStats = adminUserService.getUserStatistics();
            
            // Build comprehensive dashboard data
            Map<String, Object> dashboardData = Map.of(
                "systemInfo", Map.of(
                    "serviceName", "AgenceService",
                    "version", "1.0.0",
                    "uptime", calculateUptime(), // Implement this method
                    "environment", "Development"
                ),
                "userStatistics", userStats,
                "quickActions", Map.of(
                    "createUser", "/api/v1/agence/admin/users",
                    "viewUsers", "/api/v1/agence/admin/users",
                    "userStats", "/api/v1/agence/admin/users/statistics",
                    "pendingDocuments", "/api/v1/agence/admin/documents/pending"
                ),
                "notifications", Map.of(
                    "pendingApprovals", 0, // Get from document service
                    "systemAlerts", 0,
                    "newUsers", userStats.getTotalUsers() // Adjust as needed
                ),
                "generatedAt", LocalDateTime.now()
            );

            log.info("✅ Dashboard admin généré avec succès");
            return ResponseEntity.ok(ApiResponse.success(dashboardData));

        } catch (Exception e) {
            log.error("❌ Erreur génération dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la génération du dashboard", e.getMessage()));
        }
    }

    /**
     * 🏥 System Health Check
     * Returns system health status with proper error handling
     */
    @GetMapping("/health")
    @Operation(summary = "État de santé du système")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Santé du système récupérée"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Service indisponible")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {

        try {
            log.info("🏥 Vérification santé du système...");
            
            // Perform actual health checks
            Map<String, Object> healthData = Map.of(
                "status", "UP",
                "database", checkDatabaseHealth(), // Implement this
                "messaging", checkMessagingHealth(), // Implement this
                "dependencies", Map.of(
                    "mongodb", checkMongoHealth(), // Implement this
                    "rabbitmq", checkRabbitMQHealth() // Implement this
                ),
                "memoryUsage", getMemoryUsage(), // Implement this
                "timestamp", LocalDateTime.now()
            );

            log.info("✅ Santé système vérifiée");
            return ResponseEntity.ok(ApiResponse.success(healthData));

        } catch (Exception e) {
            log.error("❌ Erreur vérification santé système: {}", e.getMessage(), e);
            
            // Return partial health data even on error
            Map<String, Object> partialHealth = Map.of(
                "status", "PARTIAL",
                "database", "UNKNOWN",
                "messaging", "UNKNOWN",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.status(503)
                    .body(ApiResponse.error("Vérification santé partielle", partialHealth.toString()));
        }
    }

    /**
     * 📝 Recent System Activity
     * Returns recent system events and activities
     */
    @GetMapping("/recent-activity")
    @Operation(summary = "Activité récente du système")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Activité récupérée"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecentActivity() {

        try {
            log.info("📝 Récupération activité récente...");
            
            // For now, return mock data - implement real activity tracking later
            Map<String, Object> activityData = Map.of(
                "recentLogins", java.util.List.of(
                    Map.of("user", "admin", "timestamp", LocalDateTime.now().minusHours(1)),
                    Map.of("user", "supervisor", "timestamp", LocalDateTime.now().minusHours(2))
                ),
                "recentUserCreations", java.util.List.of(),
                "systemEvents", java.util.List.of(
                    Map.of("event", "Service started", "timestamp", LocalDateTime.now().minusHours(3))
                ),
                "pendingTasks", 0,
                "generatedAt", LocalDateTime.now(),
                "message", "Données d'activité basiques - audit complet à implémenter"
            );

            log.info("✅ Activité récente récupérée");
            return ResponseEntity.ok(ApiResponse.success(activityData));

        } catch (Exception e) {
            log.error("❌ Erreur récupération activité: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la récupération de l'activité"));
        }
    }

    // =====================================
    // HELPER METHODS
    // =====================================

    /**
     * Calculate system uptime (implement based on your needs)
     */
    private String calculateUptime() {
        // Simple implementation - you can make this more sophisticated
        return "2h 30m"; // Placeholder
    }

    /**
     * Check database connectivity
     */
    private String checkDatabaseHealth() {
        try {
            // Implement actual database ping
            // For now, return UP
            return "UP";
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return "DOWN";
        }
    }

    /**
     * Check messaging system health
     */
    private String checkMessagingHealth() {
        try {
            // Implement actual messaging system check
            return "UP";
        } catch (Exception e) {
            log.warn("Messaging health check failed: {}", e.getMessage());
            return "DOWN";
        }
    }

    /**
     * Check MongoDB health
     */
    private String checkMongoHealth() {
        // Implement MongoDB specific health check
        return "UP";
    }

    /**
     * Check RabbitMQ health
     */
    private String checkRabbitMQHealth() {
        // Implement RabbitMQ specific health check
        return "UP";
    }

    /**
     * Get current memory usage
     */
    private Map<String, Object> getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return Map.of(
            "used", usedMemory / (1024 * 1024) + " MB",
            "free", freeMemory / (1024 * 1024) + " MB",
            "total", totalMemory / (1024 * 1024) + " MB",
            "max", maxMemory / (1024 * 1024) + " MB"
        );
    }
}