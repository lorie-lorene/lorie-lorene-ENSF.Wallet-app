package com.m1_fonda.serviceUser.web.controler;

import com.m1_fonda.serviceUser.pojo.ClientStatisticsDTO;
import com.m1_fonda.serviceUser.pojo.ClientSummaryDTO;
import com.m1_fonda.serviceUser.response.ApiResponse;
import com.m1_fonda.serviceUser.service.AdminClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 👥 Admin Controller for ServiceUser
 * 
 * Provides administrative endpoints for client management and statistics
 * Used by AgenceService dashboard to fetch real client data
 */
@RestController
@RequestMapping("/api/v1/users/admin")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
@Slf4j
@Tag(name = "User Admin", description = "Administration des clients")
public class AdminController {

    @Autowired
    private AdminClientService adminClientService;

/**
 * 👥 Admin Controller for UserService
 * 
 * Provides administrative endpoints for client management and statistics
 * Used by AgenceService dashboard to fetch real client data
 */
    @GetMapping("/statistics")
    @Operation(summary = "Statistiques complètes des clients")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistiques récupérées"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    public ResponseEntity<ApiResponse<ClientStatisticsDTO>> getClientStatistics() {
        
        try {
            log.info("📊 Génération statistiques clients...");
            
            ClientStatisticsDTO statistics = adminClientService.getClientStatistics();
            
            log.info("📊 Statistiques générées: {} clients totaux", statistics.getTotalClients());
            return ResponseEntity.ok(ApiResponse.success(statistics));
            
        } catch (Exception e) {
            log.error("❌ Erreur génération statistiques clients: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la génération des statistiques"));
        }
    }

    /**
     * 👥 Get paginated list of all clients
     * For detailed client management in dashboard
     */
    @GetMapping("/clients")
    @Operation(summary = "Liste paginée des clients")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Liste récupérée"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<ApiResponse<Page<ClientSummaryDTO>>> getAllClients(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String agenceId) {

        try {
            log.info("👥 Récupération liste clients - page: {}, size: {}, status: {}, search: {}", 
                    pageable.getPageNumber(), pageable.getPageSize(), status, search);

            Page<ClientSummaryDTO> clients = adminClientService.getAllClients(
                    pageable, status, search, agenceId);

            log.info("👥 {} clients trouvés", clients.getTotalElements());
            return ResponseEntity.ok(ApiResponse.success(clients));

        } catch (Exception e) {
            log.error("❌ Erreur récupération clients: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la récupération des clients"));
        }
    }

    /**
     * 🔍 Search clients by various criteria
     */
    @GetMapping("/clients/search")
    @Operation(summary = "Recherche avancée de clients")
    public ResponseEntity<ApiResponse<Page<ClientSummaryDTO>>> searchClients(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            log.info("🔍 Recherche clients: {}", query);
            
            Page<ClientSummaryDTO> results = adminClientService.searchClients(query, pageable);
            
            log.info("🔍 {} résultats trouvés pour: {}", results.getTotalElements(), query);
            return ResponseEntity.ok(ApiResponse.success(results));
            
        } catch (Exception e) {
            log.error("❌ Erreur recherche clients: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la recherche"));
        }
    }

    /**
     * 📈 Get client statistics by agency
     */
    @GetMapping("/statistics/by-agency")
    @Operation(summary = "Statistiques par agence")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatisticsByAgency() {
        
        try {
            log.info("📈 Génération statistiques par agence...");
            
            Map<String, Object> agencyStats = adminClientService.getStatisticsByAgency();
            
            log.info("📈 Statistiques par agence générées");
            return ResponseEntity.ok(ApiResponse.success(agencyStats));
            
        } catch (Exception e) {
            log.error("❌ Erreur statistiques par agence: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la génération des statistiques par agence"));
        }
    }

    /**
     * 🕒 Get recent client activity
     */
    @GetMapping("/activity/recent")
    @Operation(summary = "Activité récente des clients")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecentActivity() {
        
        try {
            log.info("🕒 Récupération activité récente...");
            
            Map<String, Object> recentActivity = adminClientService.getRecentActivity();
            
            log.info("🕒 Activité récente récupérée");
            return ResponseEntity.ok(ApiResponse.success(recentActivity));
            
        } catch (Exception e) {
            log.error("❌ Erreur activité récente: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la récupération de l'activité récente"));
        }
    }
}