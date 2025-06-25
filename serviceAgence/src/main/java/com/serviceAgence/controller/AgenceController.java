package com.serviceAgence.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.serviceAgence.dto.*;
import com.serviceAgence.model.Agence;
import com.serviceAgence.model.CompteUser;
import com.serviceAgence.model.Transaction;
import com.serviceAgence.services.AgenceService;
import com.serviceAgence.services.FraisService;
import com.serviceAgence.services.KYCService;
import com.serviceAgence.services.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/agence")
@Validated
@Slf4j
@Tag(name = "Agence", description = "API de gestion des agences et comptes")
public class AgenceController {

    @Autowired
    private AgenceService agenceService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private KYCService kycService;

    @Autowired
    private FraisService fraisService;

    /**
     * Récupération de tous les comptes d'une agence
     */
    @GetMapping("/{idAgence}/comptes")
    @PreAuthorize("hasRole('AGENCE') or hasRole('ADMIN')")
    @Operation(summary = "Lister les comptes d'une agence")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "Agence introuvable"),
            @ApiResponse(responseCode = "403", description = "Accès non autorisé")
    })
    public ResponseEntity<List<CompteUser>> getAgenceAccounts(
            @PathVariable @NotBlank String idAgence,
            @RequestParam(defaultValue = "50") @Min(1) int limit) {

        try {
            List<CompteUser> comptes = agenceService.getAgenceAccounts(idAgence, limit);

            log.info("Récupération de {} comptes pour agence: {}", comptes.size(), idAgence);
            return ResponseEntity.ok(comptes);

        } catch (Exception e) {
            log.error("Erreur récupération comptes agence {}: {}", idAgence, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Recherche d'un compte par numéro
     */
    @GetMapping("/comptes/{numeroCompte}")
    @PreAuthorize("hasRole('AGENCE') or hasRole('ADMIN')")
    @Operation(summary = "Rechercher un compte par numéro")
    public ResponseEntity<CompteUser> findAccountByNumber(
            @PathVariable @NotBlank String numeroCompte) {

        try {
            CompteUser compte = agenceService.findAccountByNumber(numeroCompte);
            return ResponseEntity.ok(compte);

        } catch (Exception e) {
            log.warn("Compte {} introuvable: {}", numeroCompte, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Récupération du solde d'un compte
     */
    @GetMapping("/comptes/{numeroCompte}/solde")
    @PreAuthorize("hasRole('AGENCE') or hasRole('ADMIN') or hasRole('CLIENT')")
    @Operation(summary = "Consulter le solde d'un compte")
    public ResponseEntity<Map<String, Object>> getAccountBalance(
            @PathVariable @NotBlank String numeroCompte) {

        try {
            BigDecimal solde = transactionService.getAccountBalance(numeroCompte);

            Map<String, Object> response = Map.of(
                    "numeroCompte", numeroCompte,
                    "solde", solde,
                    "devise", "FCFA",
                    "timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur récupération solde {}: {}", numeroCompte, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Historique des transactions d'un compte
     */
    @GetMapping("/comptes/{numeroCompte}/transactions")
    @PreAuthorize("hasRole('AGENCE') or hasRole('ADMIN')")
    @Operation(summary = "Historique des transactions d'un compte")
    public ResponseEntity<List<Transaction>> getAccountHistory(
            @PathVariable @NotBlank String numeroCompte,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {

        try {
            List<Transaction> transactions = transactionService.getAccountHistory(numeroCompte, limit);

            log.info("Récupération de {} transactions pour compte: {}", transactions.size(), numeroCompte);
            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            log.error("Erreur récupération historique {}: {}", numeroCompte, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Exécution d'une transaction manuelle par l'agence
     */
    @PostMapping("/transactions")
    @PreAuthorize("hasRole('AGENCE')")
    @Operation(summary = "Exécuter une transaction manuelle")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction réussie"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "403", description = "Accès non autorisé")
    })
    public ResponseEntity<TransactionResult> processTransaction(
            @Valid @RequestBody TransactionRequest request) {

        try {
            log.info("Traitement transaction agence: type={}, montant={}, compte={}",
                    request.getType(), request.getMontant(), request.getCompteSource());

            TransactionResult result = agenceService.processTransaction(request);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("Erreur traitement transaction: {}", e.getMessage(), e);
            TransactionResult errorResult = TransactionResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors du traitement");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Estimation des frais d'une transaction
     */
    @PostMapping("/transactions/estimate-frais")
    @PreAuthorize("hasRole('AGENCE') or hasRole('CLIENT')")
    @Operation(summary = "Estimer les frais d'une transaction")
    public ResponseEntity<Map<String, BigDecimal>> estimateTransactionFees(
            @RequestBody @Valid TransactionRequest request) {

        try {
            Map<String, BigDecimal> estimation = fraisService.estimateFrais(
                    request.getType(), request.getMontant(), request.getIdAgence());

            return ResponseEntity.ok(estimation);

        } catch (Exception e) {
            log.error("Erreur estimation frais: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Activation d'un compte
     */
    @PutMapping("/comptes/{numeroCompte}/activate")
    @PreAuthorize("hasRole('AGENCE')")
    @Operation(summary = "Activer un compte")
    public ResponseEntity<Map<String, String>> activateAccount(
            @PathVariable @NotBlank String numeroCompte,
            @RequestParam @NotBlank String activatedBy) {

        try {
            agenceService.activateAccount(numeroCompte, activatedBy);

            Map<String, String> response = Map.of(
                    "status", "SUCCESS",
                    "message", "Compte activé avec succès",
                    "numeroCompte", numeroCompte);

            log.info("Compte activé: {} par {}", numeroCompte, activatedBy);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur activation compte {}: {}", numeroCompte, e.getMessage());

            Map<String, String> response = Map.of(
                    "status", "ERROR",
                    "message", e.getMessage(),
                    "numeroCompte", numeroCompte);

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Suspension d'un compte
     */
    @PutMapping("/comptes/{numeroCompte}/suspend")
    @PreAuthorize("hasRole('AGENCE')")
    @Operation(summary = "Suspendre un compte")
    public ResponseEntity<Map<String, String>> suspendAccount(
            @PathVariable @NotBlank String numeroCompte,
            @RequestParam @NotBlank String reason,
            @RequestParam @NotBlank String suspendedBy) {

        try {
            agenceService.suspendAccount(numeroCompte, reason, suspendedBy);

            Map<String, String> response = Map.of(
                    "status", "SUCCESS",
                    "message", "Compte suspendu avec succès",
                    "numeroCompte", numeroCompte,
                    "reason", reason);

            log.info("Compte suspendu: {} par {} - Raison: {}", numeroCompte, suspendedBy, reason);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur suspension compte {}: {}", numeroCompte, e.getMessage());

            Map<String, String> response = Map.of(
                    "status", "ERROR",
                    "message", e.getMessage(),
                    "numeroCompte", numeroCompte);

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Validation manuelle des documents KYC
     */
    @PostMapping("/kyc/validate")
    @PreAuthorize("hasRole('AGENCE')")
    @Operation(summary = "Valider manuellement des documents KYC")
    public ResponseEntity<KYCValidationResult> validateKYCDocuments(
            @RequestParam @NotBlank String idClient,
            @RequestParam @NotBlank String cni,
            @RequestParam(required = false) byte[] rectoCni,
            @RequestParam(required = false) byte[] versoCni,
            @RequestParam(required = false) byte[] selfie) {

        try {
            log.info("Validation KYC manuelle pour client: {}", idClient);

            KYCValidationResult result = kycService.validateDocumentsWithSelfie(idClient, cni, rectoCni, versoCni, selfie);

            if (result.isValid()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("Erreur validation KYC: {}", e.getMessage(), e);
            KYCValidationResult errorResult = KYCValidationResult.rejected("ERREUR_TECHNIQUE",
                    "Erreur technique lors de la validation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Génération du rapport KYC d'un client
     */
    @GetMapping("/kyc/{idClient}/report")
    @PreAuthorize("hasRole('AGENCE') or hasRole('ADMIN')")
    @Operation(summary = "Générer le rapport KYC d'un client")
    public ResponseEntity<String> generateKYCReport(@PathVariable @NotBlank String idClient) {

        try {
            String report = kycService.generateKYCReport(idClient);

            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .body(report);

        } catch (Exception e) {
            log.error("Erreur génération rapport KYC: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Erreur lors de la génération du rapport");
        }
    }

    /**
     * Statistiques générales d'une agence
     */
    @GetMapping("/{idAgence}/statistics")
    @PreAuthorize("hasRole('AGENCE') or hasRole('ADMIN')")
    @Operation(summary = "Statistiques d'une agence")
    public ResponseEntity<AgenceStatistics> getAgenceStatistics(@PathVariable @NotBlank String idAgence) {

        try {
            AgenceStatistics stats = agenceService.getAgenceStatistics(idAgence);

            log.info("Statistiques récupérées pour agence: {}", idAgence);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Erreur récupération statistiques agence {}: {}", idAgence, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Informations générales d'une agence
     */
    @GetMapping("/{idAgence}/info")
    @Operation(summary = "Informations d'une agence")
    public ResponseEntity<Agence> getAgenceInfo(@PathVariable @NotBlank String idAgence) {

        try {
            Agence agence = agenceService.getAgenceInfo(idAgence);

            return ResponseEntity.ok(agence);

        } catch (Exception e) {
            log.error("Erreur récupération info agence {}: {}", idAgence, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Recherche de comptes avec filtres
     */
    @GetMapping("/{idAgence}/comptes/search")
    @PreAuthorize("hasRole('AGENCE') or hasRole('ADMIN')")
    @Operation(summary = "Rechercher des comptes avec filtres")
    public ResponseEntity<List<CompteUser>> searchAccounts(
            @PathVariable @NotBlank String idAgence,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            List<CompteUser> comptes = agenceService.getAgenceAccounts(idAgence, size);

            // Filtrage simple par statut
            if (status != null && !status.trim().isEmpty()) {
                comptes = comptes.stream()
                        .filter(compte -> compte.getStatus().toString().equalsIgnoreCase(status))
                        .toList();
            }

            // Filtrage par client
            if (clientId != null && !clientId.trim().isEmpty()) {
                comptes = comptes.stream()
                        .filter(compte -> compte.getIdClient().equals(clientId))
                        .toList();
            }

            return ResponseEntity.ok(comptes);

        } catch (Exception e) {
            log.error("Erreur recherche comptes: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Vérification de la santé du service
     */
    @GetMapping("/health")
    @Operation(summary = "Vérifier la santé du service")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "AgenceService",
                "version", "2.0.0",
                "timestamp", java.time.LocalDateTime.now());

        return ResponseEntity.ok(health);
    }

    /**
     * Configuration des frais par type de transaction
     */
    @GetMapping("/config/frais")
    @Operation(summary = "Configuration des frais")
    public ResponseEntity<Map<String, Object>> getFraisConfiguration() {
        try {
            // Retourner la configuration des frais par défaut
            Map<String, Object> config = Map.of(
                    "fraisDepotPhysique", "0%",
                    "fraisRetraitPhysique", "1.5% (min 100 FCFA)",
                    "fraisRetraitMobileMoney", "1.5% (min 150 FCFA)",
                    "fraisTransfertInterne", "1% (min 50 FCFA)",
                    "fraisTransfertExterne", "2% (min 500 FCFA)",
                    "tva", "17.5%",
                    "fraisTenueCompte", "500 FCFA/mois");

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            log.error("Erreur récupération config frais: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Validation des limites d'une agence
     */
    @PostMapping("/{idAgence}/validate-limits")
    @PreAuthorize("hasRole('AGENCE')")
    @Operation(summary = "Valider les limites d'une agence pour une transaction")
    public ResponseEntity<Map<String, Object>> validateAgenceLimits(
            @PathVariable @NotBlank String idAgence,
            @RequestParam BigDecimal montant) {

        try {
            boolean valid = agenceService.validateAgenceLimits(idAgence, montant);

            Map<String, Object> response = Map.of(
                    "valid", valid,
                    "montant", montant,
                    "agence", idAgence,
                    "timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur validation limites agence: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ===================================
    // GESTION D'ERREURS AU NIVEAU CONTRÔLEUR
    // ===================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = Map.of(
                "error", "INVALID_ARGUMENT",
                "message", ex.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Erreur runtime dans contrôleur: {}", ex.getMessage(), ex);

        Map<String, String> error = Map.of(
                "error", "RUNTIME_ERROR",
                "message", "Erreur technique",
                "timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
