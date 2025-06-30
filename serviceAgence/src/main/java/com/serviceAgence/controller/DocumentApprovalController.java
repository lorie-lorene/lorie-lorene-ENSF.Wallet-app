package com.serviceAgence.controller;

import com.serviceAgence.dto.document.*;
import com.serviceAgence.dto.common.ApiResponse;
import com.serviceAgence.enums.DocumentStatus;
import com.serviceAgence.services.DocumentApprovalService;
import io.swagger.v3.oas.annotations.Operation;
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
import com.serviceAgence.exception.AuthenticationException;

import java.time.LocalDate;
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
    public ResponseEntity<ApiResponse<Page<PendingDocumentDTO>>> getPendingDocuments(
            @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable,
            @RequestParam(required = false) String agencyFilter,
            @RequestParam(required = false) String typeFilter) {

        try {
            log.info("📋 Récupération documents en attente - page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());

            // ✅ USE REAL SERVICE instead of mock data
            Page<PendingDocumentDTO> pendingDocuments = documentApprovalService
                    .getPendingDocuments(pageable, agencyFilter);

            log.info("📋 {} documents en attente trouvés", pendingDocuments.getTotalElements());
            return ResponseEntity.ok(ApiResponse.success(pendingDocuments));

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
    @Operation(summary = "Récupérer un document pour review détaillée")
    public ResponseEntity<ApiResponse> getDocumentForReview(@PathVariable String documentId) {
        try {
            log.info("👁️ Récupération document pour review: {}", documentId);

            // ✅ USE REAL SERVICE instead of mock
            DocumentReviewDTO documentReview = documentApprovalService
                    .getDocumentForReview(documentId);

            log.info("👁️ Document récupéré pour review: {} - Type: {}", 
                    documentId, documentReview.getStatus());
            
            return ResponseEntity.ok(ApiResponse.success(documentReview));

        } catch (AuthenticationException e) {
            log.warn("❌ Document introuvable ou non accessible: {}", documentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ Erreur récupération document {}: {}", documentId, e.getMessage());
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
            @Valid @RequestBody DocumentApprovalRequest approvalRequest,
            Authentication authentication) {

        try {
            // ✅ GET REAL ADMIN INFO from security context
            String approvedBy = authentication.getName();
            log.info("✅ Approbation document: {} par {}", documentId, approvedBy);

            // ✅ USE REAL SERVICE METHOD with proper DTO
            DocumentApprovalResult result = documentApprovalService
                    .approveDocument(documentId, approvalRequest, approvedBy);

            log.info("✅ Document approuvé avec succès: {} - Statut: {}", 
                    documentId, result.getStatus());
            
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (AuthenticationException e) {
            log.warn("❌ Erreur validation document {}: {}", documentId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur approbation document {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de l'approbation du document"));
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
    public ResponseEntity<ApiResponse> getDocumentStatistics(
            @RequestParam(required = false) String agenceFilter,
            @RequestParam(required = false) LocalDate dateStart,
            @RequestParam(required = false) LocalDate dateEnd) {

        try {
            log.info("📊 Récupération statistiques documents");

            // ✅ USE REAL SERVICE instead of mock data
            DocumentStatisticsDTO statistics = documentApprovalService
                    .getDocumentStatistics();
            log.info("statistics: {}", statistics);
            log.info("📊 Statistiques récupérées pour l'agence: {}", agenceFilter);

            log.info("📊 Statistiques calculées: total={}, pending={}, approved={}, rejected={}", 
                    statistics.getTotalDocuments(), 
                    statistics.getPendingDocuments(),
                    statistics.getApprovedDocuments(), 
                    statistics.getRejectedDocuments());
            
            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            log.error("❌ Erreur récupération statistiques: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la récupération des statistiques"));
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

    @PostMapping("/{documentId}/reject")
    @Operation(summary = "Rejeter un document")
    public ResponseEntity<ApiResponse> rejectDocument(
            @PathVariable String documentId,
            @Valid @RequestBody DocumentRejectionRequest rejectionRequest,
            Authentication authentication) {

        try {
            // ✅ GET REAL ADMIN INFO from security context
            String rejectedBy = authentication.getName();
            log.info("❌ Rejet document: {} par {} - Raison: {}", 
                    documentId, rejectedBy, rejectionRequest.getReason());

            // ✅ USE REAL SERVICE METHOD with proper DTO
            DocumentApprovalResult result = documentApprovalService
                    .rejectDocument(documentId, rejectionRequest, rejectedBy);

            log.info("❌ Document rejeté avec succès: {} - Statut: {}", 
                    documentId, result.getStatus());
            
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (AuthenticationException e) {
            log.warn("❌ Erreur validation document {}: {}", documentId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur rejet document {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors du rejet du document"));
        }
    }

    /**
     * ✅ Bulk Approve Documents
     */
    @PostMapping("/bulk-approve")
    @Operation(summary = "Approbation en lot de documents")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Documents approuvés en lot"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<ApiResponse> bulkApproveDocuments(
            @Valid @RequestBody BulkDocumentRequest bulkRequest,
            Authentication authentication) {

        try {
            String approvedBy = authentication.getName();
            log.info("✅ Approbation en lot: {} documents par {}", 
                    bulkRequest.getDocumentIds().size(), approvedBy);

            // Validate input
            if (bulkRequest.getDocumentIds() == null || bulkRequest.getDocumentIds().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Liste de documents vide"));
            }

            // Process bulk approval
            BulkOperationResult result = documentApprovalService
                    .bulkApproveDocuments(bulkRequest.getDocumentIds(), 
                                        bulkRequest.getComment(), 
                                        approvedBy);

            log.info("✅ Approbation en lot terminée: {} succès, {} échecs", 
                    result.getSuccessCount(), result.getFailureCount());

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("❌ Erreur approbation en lot: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de l'approbation en lot: " + e.getMessage()));
        }
    }

    /**
     * ❌ Bulk Reject Documents
     */
    @PostMapping("/bulk-reject")
    @Operation(summary = "Rejet en lot de documents")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Documents rejetés en lot"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<ApiResponse> bulkRejectDocuments(
            @Valid @RequestBody BulkDocumentRequest bulkRequest,
            Authentication authentication) {

        try {
            String rejectedBy = authentication.getName();
            log.info("❌ Rejet en lot: {} documents par {} - Raison: {}", 
                    bulkRequest.getDocumentIds().size(), rejectedBy, bulkRequest.getReason());

            // Validate input
            if (bulkRequest.getDocumentIds() == null || bulkRequest.getDocumentIds().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Liste de documents vide"));
            }

            if (bulkRequest.getReason() == null || bulkRequest.getReason().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Raison du rejet requise"));
            }

            // Process bulk rejection
            BulkOperationResult result = documentApprovalService
                    .bulkRejectDocuments(bulkRequest.getDocumentIds(), 
                                    bulkRequest.getReason(), 
                                    rejectedBy);

            log.info("❌ Rejet en lot terminé: {} succès, {} échecs", 
                    result.getSuccessCount(), result.getFailureCount());

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("❌ Erreur rejet en lot: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors du rejet en lot: " + e.getMessage()));
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