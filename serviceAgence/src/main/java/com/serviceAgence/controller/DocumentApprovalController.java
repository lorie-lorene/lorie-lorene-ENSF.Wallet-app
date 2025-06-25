package com.serviceAgence.controller;

import com.serviceAgence.dto.document.*;
import com.serviceAgence.dto.common.ApiResponse;
import com.serviceAgence.enums.DocumentStatus;
import com.serviceAgence.services.DocumentApprovalService;
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
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
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
    @Operation(summary = "Liste des documents en attente d'approbation")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Liste récupérée"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<ApiResponse> getPendingDocuments(
            @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable,
            @RequestParam(required = false) String agencyFilter,
            @RequestParam(required = false) String typeFilter) {

        try {
            log.info("📋 Récupération documents en attente - page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());

            // For now, return mock data - implement real document service integration
            List<Map<String, Object>> mockDocuments = List.of(
                Map.of(
                    "id", "DOC_001",
                    "clientId", "CLIENT_123",
                    "clientName", "Jean Dupont",
                    "documentType", "CNI",
                    "status", "PENDING",
                    "submittedAt", LocalDateTime.now().minusDays(1),
                    "agencyCode", "AGENCE001",
                    "priority", "NORMAL",
                    "fileUrl", "/documents/cni_123.pdf"
                ),
                Map.of(
                    "id", "DOC_002", 
                    "clientId", "CLIENT_456",
                    "clientName", "Marie Martin",
                    "documentType", "PASSPORT",
                    "status", "PENDING",
                    "submittedAt", LocalDateTime.now().minusDays(2),
                    "agencyCode", "AGENCE002",
                    "priority", "HIGH",
                    "fileUrl", "/documents/passport_456.pdf"
                )
            );

            // Build paginated response
            Map<String, Object> paginatedResponse = Map.of(
                "content", mockDocuments,
                "totalElements", 25L,
                "totalPages", 2,
                "size", pageable.getPageSize(),
                "number", pageable.getPageNumber(),
                "first", pageable.getPageNumber() == 0,
                "last", pageable.getPageNumber() == 1,
                "numberOfElements", mockDocuments.size()
            );

            log.info("✅ Documents en attente récupérés: {} documents", mockDocuments.size());
            return ResponseEntity.ok(ApiResponse.success(paginatedResponse));

        } catch (Exception e) {
            log.error("❌ Erreur récupération documents en attente: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la récupération des documents"));
        }
    }

    /**
     * Détails d'un document pour review avec images
     */
    @GetMapping("/{documentId}/review")
    @Operation(summary = "Récupérer un document pour review")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Document récupéré pour review"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Document introuvable"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Document non disponible pour review")
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
     * ✅ Approve Document
     */
    @PostMapping("/{documentId}/approve")
    @Operation(summary = "Approuver un document")
    public ResponseEntity<ApiResponse> approveDocument(
            @PathVariable String documentId,
            @RequestBody Map<String, Object> approvalData) {

        try {
            log.info("✅ Approbation document: {}", documentId);

            // Mock approval process
            Map<String, Object> result = Map.of(
                "documentId", documentId,
                "status", "APPROVED",
                "approvedBy", "current-admin", // Get from security context
                "approvedAt", LocalDateTime.now(),
                "comments", approvalData.getOrDefault("comments", "")
            );

            log.info("✅ Document approuvé: {}", documentId);
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("❌ Erreur approbation document {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de l'approbation du document"));
        }
    }

   /**
     * ❌ Reject Document
     */
    @PostMapping("/{documentId}/reject")
    @Operation(summary = "Rejeter un document")
    public ResponseEntity<ApiResponse> rejectDocument(
            @PathVariable String documentId,
            @RequestBody Map<String, Object> rejectionData) {

        try {
            log.info("❌ Rejet document: {}", documentId);

            // Mock rejection process
            Map<String, Object> result = Map.of(
                "documentId", documentId,
                "status", "REJECTED",
                "rejectedBy", "current-admin", // Get from security context
                "rejectedAt", LocalDateTime.now(),
                "reason", rejectionData.getOrDefault("reason", "Non spécifié"),
                "comments", rejectionData.getOrDefault("comments", "")
            );

            log.info("❌ Document rejeté: {}", documentId);
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("❌ Erreur rejet document {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors du rejet du document"));
        }
    }
    /**
     * Retourner un document en attente (annuler review)
     */
    @PostMapping("/{documentId}/return")
    @Operation(summary = "Retourner un document en attente")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Document remis en attente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Document introuvable"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Document non en cours de review")
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historique récupéré"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistiques récupérées"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Résultats de recherche"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Paramètres de recherche invalides")
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

    // /**
    //  * 🔍 Get Document Details for Review
    //  */
    // @GetMapping("/{documentId}/review")
    // @Operation(summary = "Détails d'un document pour révision")
    // public ResponseEntity<ApiResponse> getDocumentForReview(
    //         @PathVariable String documentId) {

    //     try {
    //         log.info("🔍 Récupération détails document pour révision: {}", documentId);

    //         // Mock document details
    //         Map<String, Object> documentDetails = Map.of(
    //             "id", documentId,
    //             "clientId", "CLIENT_123",
    //             "clientInfo", Map.of(
    //                 "name", "Jean Dupont",
    //                 "email", "jean.dupont@email.com",
    //                 "phone", "+237123456789"
    //             ),
    //             "documentType", "CNI",
    //             "status", "PENDING",
    //             "submittedAt", LocalDateTime.now().minusDays(1),
    //             "agencyCode", "AGENCE001",
    //             "fileUrl", "/documents/cni_123.pdf",
    //             "fileSize", "2.5 MB",
    //             "metadata", Map.of(
    //                 "uploadedFrom", "Mobile App",
    //                 "ipAddress", "192.168.1.100",
    //                 "userAgent", "AgenceApp/1.0"
    //             )
    //         );

    //         log.info("✅ Détails document récupérés: {}", documentId);
    //         return ResponseEntity.ok(ApiResponse.success(documentDetails));

    //     } catch (Exception e) {
    //         log.error("❌ Erreur récupération détails document {}: {}", documentId, e.getMessage(), e);
    //         return ResponseEntity.notFound().build();
    //     }
    // }
}