package com.serviceAgence.controller;

import com.serviceAgence.dto.document.*;
import com.serviceAgence.enums.DocumentStatus;
import com.serviceAgence.services.DocumentApprovalService;
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
 * Contrôleur pour l'approbation manuelle des documents KYC
 * Permet aux admins et superviseurs de reviewer et approuver/rejeter les documents clients
 */
@RestController
@RequestMapping("/api/v1/agence/admin/documents")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
@Validated
@Slf4j
@Tag(name = "Document Approval", description = "Workflow d'approbation des documents KYC")
public class DocumentApprovalController {

    @Autowired
    private DocumentApprovalService documentApprovalService;

    /**
     * Liste des documents en attente d'approbation
     */
    @GetMapping("/pending")
    @Operation(summary = "Documents en attente d'approbation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste récupérée"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<Page<PendingDocumentDTO>> getPendingDocuments(
            @PageableDefault(size = 20, sort = "uploadedAt") Pageable pageable,
            @RequestParam(required = false) String agence) {

        try {
            Page<PendingDocumentDTO> pendingDocuments = 
                    documentApprovalService.getPendingDocuments(pageable, agence);

            log.info("📋 Documents en attente récupérés: {} documents", pendingDocuments.getTotalElements());
            return ResponseEntity.ok(pendingDocuments);

        } catch (Exception e) {
            log.error("❌ Erreur récupération documents en attente: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Détails d'un document pour review avec images
     */
    @GetMapping("/{documentId}/review")
    @Operation(summary = "Récupérer un document pour review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document récupéré pour review"),
        @ApiResponse(responseCode = "404", description = "Document introuvable"),
        @ApiResponse(responseCode = "400", description = "Document non disponible pour review")
    })
    public ResponseEntity<DocumentReviewDTO> getDocumentForReview(@PathVariable String documentId) {

        try {
            DocumentReviewDTO documentReview = documentApprovalService.getDocumentForReview(documentId);

            log.info("🔍 Document récupéré pour review: {}", documentId);
            return ResponseEntity.ok(documentReview);

        } catch (Exception e) {
            log.error("❌ Erreur récupération document pour review {}: {}", documentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Approuver un document
     */
    @PostMapping("/{documentId}/approve")
    @Operation(summary = "Approuver un document")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document approuvé"),
        @ApiResponse(responseCode = "404", description = "Document introuvable"),
        @ApiResponse(responseCode = "400", description = "Document non en cours de review")
    })
    public ResponseEntity<DocumentApprovalResult> approveDocument(
            @PathVariable String documentId,
            @Valid @RequestBody DocumentApprovalRequest request,
            Authentication authentication) {

        try {
            String approvedBy = authentication.getName();
            DocumentApprovalResult result = documentApprovalService.approveDocument(
                    documentId, request, approvedBy);

            log.info("✅ Document approuvé: {} par {}", documentId, approvedBy);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur approbation document {}: {}", documentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Rejeter un document
     */
    @PostMapping("/{documentId}/reject")
    @Operation(summary = "Rejeter un document")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document rejeté"),
        @ApiResponse(responseCode = "404", description = "Document introuvable"),
        @ApiResponse(responseCode = "400", description = "Document non en cours de review ou données invalides")
    })
    public ResponseEntity<DocumentApprovalResult> rejectDocument(
            @PathVariable String documentId,
            @Valid @RequestBody DocumentRejectionRequest request,
            Authentication authentication) {

        try {
            String rejectedBy = authentication.getName();
            DocumentApprovalResult result = documentApprovalService.rejectDocument(
                    documentId, request, rejectedBy);

            log.info("❌ Document rejeté: {} par {} - Raison: {}", documentId, rejectedBy, request.getReason());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur rejet document {}: {}", documentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retourner un document en attente (annuler review)
     */
    @PostMapping("/{documentId}/return")
    @Operation(summary = "Retourner un document en attente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document remis en attente"),
        @ApiResponse(responseCode = "404", description = "Document introuvable"),
        @ApiResponse(responseCode = "400", description = "Document non en cours de review")
    })
    public ResponseEntity<Map<String, String>> returnDocumentToPending(
            @PathVariable String documentId,
            Authentication authentication) {

        try {
            String returnedBy = authentication.getName();
            documentApprovalService.returnDocumentToPending(documentId, returnedBy);

            Map<String, String> response = Map.of(
                "status", "SUCCESS",
                "message", "Document remis en attente",
                "documentId", documentId,
                "returnedBy", returnedBy
            );

            log.info("↩️ Document remis en attente: {} par {}", documentId, returnedBy);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur retour document en attente {}: {}", documentId, e.getMessage());
            
            Map<String, String> response = Map.of(
                "status", "ERROR",
                "message", e.getMessage(),
                "documentId", documentId
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Historique des documents traités
     */
    @GetMapping("/history")
    @Operation(summary = "Historique des documents traités")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historique récupéré"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<Page<DocumentHistoryDTO>> getDocumentHistory(
            @PageableDefault(size = 20, sort = "validatedAt") Pageable pageable,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String agence) {

        try {
            Page<DocumentHistoryDTO> history = 
                    documentApprovalService.getDocumentHistory(pageable, status, agence);

            log.info("📚 Historique documents récupéré: {} documents", history.getTotalElements());
            return ResponseEntity.ok(history);

        } catch (Exception e) {
            log.error("❌ Erreur récupération historique: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Statistiques des documents
     */
    @GetMapping("/statistics")
    @Operation(summary = "Statistiques des documents")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistiques récupérées"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<DocumentStatisticsDTO> getDocumentStatistics() {

        try {
            DocumentStatisticsDTO statistics = documentApprovalService.getDocumentStatistics();

            log.info("📊 Statistiques documents générées");
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("❌ Erreur génération statistiques documents: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Recherche de documents
     */
    @GetMapping("/search")
    @Operation(summary = "Rechercher des documents")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Résultats de recherche"),
        @ApiResponse(responseCode = "400", description = "Paramètres de recherche invalides")
    })
    public ResponseEntity<Map<String, Object>> searchDocuments(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            // Cette fonctionnalité peut être implémentée plus tard
            Map<String, Object> response = Map.of(
                "status", "INFO",
                "message", "Fonctionnalité de recherche à implémenter",
                "query", query,
                "results", "Placeholder"
            );

            log.info("🔍 Recherche documents: {}", query);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur recherche documents: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}