package com.serviceDemande.controler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.serviceDemande.dto.DashboardStats;
import com.serviceDemande.dto.ManualReviewRequest;
import com.serviceDemande.dto.TransactionLimits;
import com.serviceDemande.enums.DemandeStatus;
import com.serviceDemande.enums.RiskLevel;
import com.serviceDemande.model.Demande;
import com.serviceDemande.service.DemandeProcessingService;
import com.serviceDemande.service.SupervisionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/demande")
@Slf4j
@Tag(name = "Supervision Demandes", description = "API de supervision et gestion des demandes")
public class SupervisionController {

    @Autowired
    private SupervisionService supervisionService;

    @Autowired
    private DemandeProcessingService processingService;

    /**
     * Dashboard principal avec statistiques
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    @Operation(summary = "Dashboard des statistiques de demandes")
    public ResponseEntity<DashboardStats> getDashboard() {
        try {
            DashboardStats stats = supervisionService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur récupération dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Liste des demandes en attente de révision manuelle
     */
    @GetMapping("/manual-review/pending")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
    @Operation(summary = "Demandes en attente de révision manuelle")
    public ResponseEntity<List<Demande>> getPendingManualReviews() {
        try {
            List<Demande> pending = supervisionService.getPendingManualReviews();
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            log.error("Erreur récupération demandes en attente: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Traitement d'une révision manuelle
     */
    @PostMapping("/manual-review/{demandeId}")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
    @Operation(summary = "Traiter une révision manuelle")
    public ResponseEntity<Map<String, String>> processManualReview(
            @PathVariable String demandeId,
            @RequestBody ManualReviewRequest request) {

        try {
            processingService.processManualReview(
                    demandeId,
                    request.isApproved(),
                    request.getNotes(),
                    request.getReviewerId());

            Map<String, String> response = Map.of(
                    "status", "SUCCESS",
                    "message", "Révision traitée avec succès",
                    "demandeId", demandeId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur traitement révision manuelle: {}", e.getMessage(), e);

            Map<String, String> response = Map.of(
                    "status", "ERROR",
                    "message", "Erreur lors du traitement",
                    "demandeId", demandeId);

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Recherche de demandes avec filtres
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    @Operation(summary = "Rechercher des demandes avec filtres")
    public ResponseEntity<Page<Demande>> searchDemandes(
            @RequestParam(required = false) DemandeStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) String idAgence,
            @RequestParam(required = false) String idClient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<Demande> demandes = supervisionService.searchDemandes(
                    status, riskLevel, idAgence, idClient, pageRequest);

            return ResponseEntity.ok(demandes);

        } catch (Exception e) {
            log.error("Erreur recherche demandes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Détails d'une demande spécifique
     */
    @GetMapping("/{demandeId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    @Operation(summary = "Détails d'une demande")
    public ResponseEntity<Demande> getDemandeDetails(@PathVariable String demandeId) {
        try {
            Demande demande = supervisionService.getDemandeDetails(demandeId);
            return ResponseEntity.ok(demande);
        } catch (Exception e) {
            log.error("Erreur récupération détails demande {}: {}", demandeId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Mise à jour des limites d'un client
     */
    @PutMapping("/{demandeId}/limits")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre à jour les limites d'un client")
    public ResponseEntity<Map<String, String>> updateClientLimits(
            @PathVariable String demandeId,
            @RequestBody TransactionLimits newLimits) {

        try {
            supervisionService.updateClientLimits(demandeId, newLimits);

            Map<String, String> response = Map.of(
                    "status", "SUCCESS",
                    "message", "Limites mises à jour avec succès");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur mise à jour limites: {}", e.getMessage(), e);

            Map<String, String> response = Map.of(
                    "status", "ERROR",
                    "message", "Erreur lors de la mise à jour");

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Comptes à risque nécessitant une surveillance
     */
    @GetMapping("/high-risk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    @Operation(summary = "Comptes à risque élevé")
    public ResponseEntity<List<Demande>> getHighRiskAccounts(
            @RequestParam(defaultValue = "70") int minRiskScore) {

        try {
            List<Demande> highRiskAccounts = supervisionService.getHighRiskAccounts(minRiskScore);
            return ResponseEntity.ok(highRiskAccounts);
        } catch (Exception e) {
            log.error("Erreur récupération comptes à risque: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }

    }

    /**
     * Santé du service
     */
    @GetMapping("/health")
    @Operation(summary = "Santé du service")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "ServiceDemande",
                "version", "2.0.0",
                "timestamp", java.time.LocalDateTime.now());

        return ResponseEntity.ok(health);
    }
}
