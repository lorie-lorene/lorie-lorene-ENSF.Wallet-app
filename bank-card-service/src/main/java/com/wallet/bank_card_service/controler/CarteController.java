package com.wallet.bank_card_service.controler;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.wallet.bank_card_service.dto.CarteCreationRequest;
import com.wallet.bank_card_service.dto.CarteCreationResult;
import com.wallet.bank_card_service.dto.CarteOperationResult;
import com.wallet.bank_card_service.dto.CarteSettingsRequest;
import com.wallet.bank_card_service.dto.CarteStatistiques;
import com.wallet.bank_card_service.dto.CarteType;
import com.wallet.bank_card_service.dto.OrangeMoneyRechargeRequest;
import com.wallet.bank_card_service.dto.PinChangeRequest;
import com.wallet.bank_card_service.dto.RechargeResult;
import com.wallet.bank_card_service.dto.TransfertCarteRequest;
import com.wallet.bank_card_service.dto.TransfertCarteResult;
import com.wallet.bank_card_service.model.Carte;
import com.wallet.bank_card_service.service.CarteService;
import com.wallet.bank_card_service.service.MoneyServiceClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/cartes")
@Validated
@Slf4j
@Tag(name = "Cartes Bancaires", description = "API de gestion des cartes bancaires virtuelles et physiques")
public class CarteController {

    @Autowired
    private CarteService carteService;
    @Autowired
    private MoneyServiceClient moneyServiceClient;

    /**
     * Créer une nouvelle carte bancaire
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Créer une nouvelle carte bancaire")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Carte créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "402", description = "Solde insuffisant pour les frais"),
            @ApiResponse(responseCode = "409", description = "Limite de cartes atteinte")
    })
    public ResponseEntity<CarteCreationResult> createCarte(
            @Valid @RequestBody CarteCreationRequest request,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);
            request.setIdClient(clientId);

            log.info("🆕 Demande création carte: client={}, type={}", clientId, request.getType());

            CarteCreationResult result = carteService.createCarte(request);

            if (result.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(result);
            } else {
                HttpStatus status = getHttpStatusFromError(result.getErrorCode());
                return ResponseEntity.status(status).body(result);
            }

        } catch (Exception e) {
            log.error("❌ Erreur création carte: {}", e.getMessage(), e);
            CarteCreationResult errorResult = CarteCreationResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors de la création");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Lister toutes les cartes d'un client
     */
    @GetMapping("/my-cards")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Lister mes cartes bancaires")
    public ResponseEntity<List<Carte>> getMyCards(Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            List<Carte> cartes = carteService.getClientCards(clientId);

            log.info("📋 Récupération de {} cartes pour client: {}", cartes.size(), clientId);
            return ResponseEntity.ok(cartes);

        } catch (Exception e) {
            log.error("❌ Erreur récupération cartes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Détails d'une carte spécifique
     */
    @GetMapping("/{idCarte}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Détails d'une carte bancaire")
    public ResponseEntity<Carte> getCardDetails(
            @PathVariable @NotBlank String idCarte,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            Carte carte = carteService.getCardDetails(idCarte, clientId);

            return ResponseEntity.ok(carte);

        } catch (Exception e) {
            log.warn("⚠️ Carte {} introuvable: {}", idCarte, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Transfert d'argent du compte vers une carte
     */
    @PostMapping("/transfer-to-card")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Transférer de l'argent du compte vers une carte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfert effectué avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "402", description = "Solde insuffisant"),
            @ApiResponse(responseCode = "403", description = "Carte non autorisée")
    })
    public ResponseEntity<TransfertCarteResult> transferToCard(
            @Valid @RequestBody TransfertCarteRequest request,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("💳 Transfert vers carte: compte={}, carte={}, montant={}",
                    request.getNumeroCompteSource(), request.getIdCarteDestination(), request.getMontant());

            TransfertCarteResult result = carteService.transferToCard(request, clientId);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                HttpStatus status = getHttpStatusFromError(result.getErrorCode());
                return ResponseEntity.status(status).body(result);
            }

        } catch (Exception e) {
            log.error("❌ Erreur transfert vers carte: {}", e.getMessage(), e);
            TransfertCarteResult errorResult = TransfertCarteResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors du transfert");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Transfert d'argent d'une carte vers le compte
     */
    @PostMapping("/{idCarte}/transfer-to-account")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Transférer de l'argent d'une carte vers le compte")
    public ResponseEntity<TransfertCarteResult> transferFromCard(
            @PathVariable @NotBlank String idCarte,
            @RequestParam BigDecimal montant,
            @RequestParam(required = false) String description,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("🏦 Transfert depuis carte: carte={}, montant={}", idCarte, montant);

            TransfertCarteResult result = carteService.transferFromCard(idCarte, montant, description, clientId);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                HttpStatus status = getHttpStatusFromError(result.getErrorCode());
                return ResponseEntity.status(status).body(result);
            }

        } catch (Exception e) {
            log.error("❌ Erreur transfert depuis carte: {}", e.getMessage(), e);
            TransfertCarteResult errorResult = TransfertCarteResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors du transfert");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Bloquer une carte
     */
    @PutMapping("/{idCarte}/block")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Bloquer une carte bancaire")
    public ResponseEntity<CarteOperationResult> blockCard(
            @PathVariable @NotBlank String idCarte,
            @RequestParam String reason,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("🔒 Blocage carte: carte={}, raison={}", idCarte, reason);

            CarteOperationResult result = carteService.blockCard(idCarte, reason, clientId);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("❌ Erreur blocage carte: {}", e.getMessage(), e);
            CarteOperationResult errorResult = CarteOperationResult.failed("BLOCK", "ERREUR_TECHNIQUE",
                    "Erreur technique");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Débloquer une carte
     */
    @PutMapping("/{idCarte}/unblock")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Débloquer une carte bancaire")
    public ResponseEntity<CarteOperationResult> unblockCard(
            @PathVariable @NotBlank String idCarte,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("🔓 Déblocage carte: carte={}", idCarte);

            CarteOperationResult result = carteService.unblockCard(idCarte, clientId);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("❌ Erreur déblocage carte: {}", e.getMessage(), e);
            CarteOperationResult errorResult = CarteOperationResult.failed("UNBLOCK", "ERREUR_TECHNIQUE",
                    "Erreur technique");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Modifier les paramètres d'une carte
     */
    @PutMapping("/{idCarte}/settings")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Modifier les paramètres d'une carte")
    public ResponseEntity<CarteOperationResult> updateCardSettings(
            @PathVariable @NotBlank String idCarte,
            @Valid @RequestBody CarteSettingsRequest request,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("⚙️ Mise à jour paramètres carte: carte={}", idCarte);

            CarteOperationResult result = carteService.updateCardSettings(idCarte, request, clientId);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("❌ Erreur mise à jour paramètres: {}", e.getMessage(), e);
            CarteOperationResult errorResult = CarteOperationResult.failed("UPDATE_SETTINGS", "ERREUR_TECHNIQUE",
                    "Erreur technique");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Changer le code PIN d'une carte
     */
    @PutMapping("/{idCarte}/change-pin")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Changer le code PIN d'une carte")
    public ResponseEntity<CarteOperationResult> changePin(
            @PathVariable @NotBlank String idCarte,
            @Valid @RequestBody PinChangeRequest request,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("🔑 Changement PIN carte: carte={}", idCarte);

            CarteOperationResult result = carteService.changePin(idCarte, request, clientId);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("❌ Erreur changement PIN: {}", e.getMessage(), e);
            CarteOperationResult errorResult = CarteOperationResult.failed("CHANGE_PIN", "ERREUR_TECHNIQUE",
                    "Erreur technique");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Statistiques des cartes d'un client
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Statistiques de mes cartes bancaires")
    public ResponseEntity<CarteStatistiques> getCardStatistics(Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            CarteStatistiques stats = carteService.getClientCardStatistics(clientId);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Erreur récupération statistiques: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Transfert entre deux cartes du même client
     */
    @PostMapping("/transfer-card-to-card")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Transférer entre deux cartes")
    public ResponseEntity<TransfertCarteResult> transferBetweenCards(
            @RequestParam @NotBlank String idCarteSource,
            @RequestParam @NotBlank String idCarteDestination,
            @RequestParam BigDecimal montant,
            @RequestParam(required = false) String description,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("💳➡️💳 Transfert carte à carte: {} -> {}, montant={}",
                    idCarteSource, idCarteDestination, montant);

            // Simuler le transfert via le compte (débit carte source, crédit compte, débit
            // compte, crédit carte destination)
            // Pour simplifier, on fait un transfert direct
            TransfertCarteResult debitResult = carteService.transferFromCard(idCarteSource, montant,
                    "Transfert vers carte " + idCarteDestination, clientId);

            if (!debitResult.isSuccess()) {
                return ResponseEntity.badRequest().body(debitResult);
            }

            // Récupérer les détails de la carte destination pour avoir son compte
            Carte carteDestination = carteService.getCardDetails(idCarteDestination, clientId);

            TransfertCarteRequest creditRequest = new TransfertCarteRequest();
            creditRequest.setNumeroCompteSource(carteDestination.getNumeroCompte());
            creditRequest.setIdCarteDestination(idCarteDestination);
            creditRequest.setMontant(montant);
            creditRequest.setDescription(description != null ? description : "Transfert depuis carte " + idCarteSource);

            TransfertCarteResult creditResult = carteService.transferToCard(creditRequest, clientId);

            if (creditResult.isSuccess()) {
                return ResponseEntity.ok(creditResult);
            } else {
                return ResponseEntity.badRequest().body(creditResult);
            }

        } catch (Exception e) {
            log.error("❌ Erreur transfert carte à carte: {}", e.getMessage(), e);
            TransfertCarteResult errorResult = TransfertCarteResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors du transfert");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Historique des transactions d'une carte
     */
    @GetMapping("/{idCarte}/transactions")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Historique des transactions d'une carte")
    public ResponseEntity<List<Carte.CarteAction>> getCardTransactions(
            @PathVariable @NotBlank String idCarte,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            Carte carte = carteService.getCardDetails(idCarte, clientId);

            List<Carte.CarteAction> transactions = carte.getActionsHistory()
                    .stream()
                    .limit(limit)
                    .toList();

            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            log.error("❌ Erreur récupération historique carte: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Configuration des frais par type de carte (Info publique)
     */
    @GetMapping("/config/fees")
    @Operation(summary = "Configuration des frais par type de carte")
    public ResponseEntity<Map<String, Object>> getFeesConfiguration() {
        Map<String, Object> config = Map.of(
                "VIRTUELLE_GRATUITE", Map.of(
                        "fraisCreation", "0 FCFA",
                        "fraisMensuels", "0 FCFA",
                        "limiteDailyDefault", "500,000 FCFA",
                        "limiteMonthlyDefault", "2,000,000 FCFA"),
                "VIRTUELLE_PREMIUM", Map.of(
                        "fraisCreation", "5,000 FCFA",
                        "fraisMensuels", "1,000 FCFA",
                        "limiteDailyDefault", "2,000,000 FCFA",
                        "limiteMonthlyDefault", "10,000,000 FCFA"),
                "PHYSIQUE", Map.of(
                        "fraisCreation", "10,000 FCFA",
                        "fraisMensuels", "2,500 FCFA",
                        "limiteDailyDefault", "5,000,000 FCFA",
                        "limiteMonthlyDefault", "20,000,000 FCFA"),
                "fraisTransfert", "0.5% (min 50 FCFA, max 2,000 FCFA)",
                "limites", Map.of(
                        "maxCartesParClient", 5,
                        "maxCartesGratuites", 1));

        return ResponseEntity.ok(config);
    }

    /**
     * Simulation de création de carte (coûts et limites)
     */
    @PostMapping("/simulate-creation")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Simuler la création d'une carte")
    public ResponseEntity<Map<String, Object>> simulateCardCreation(
            @RequestParam String typecarte,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            // Récupérer le nombre de cartes existantes
            List<Carte> existingCards = carteService.getClientCards(clientId);

            CarteType type = CarteType.valueOf(typecarte.toUpperCase());

            Map<String, Object> simulation = Map.of(
                    "type", type.getLibelle(),
                    "fraisCreation", type.getFraisCreation(),
                    "fraisMensuels",
                    type == CarteType.VIRTUELLE_GRATUITE ? "0 FCFA"
                            : (type == CarteType.VIRTUELLE_PREMIUM ? "1,000 FCFA" : "2,500 FCFA"),
                    "limiteDailyDefault", type.getLimiteDailyDefault(),
                    "limiteMonthlyDefault", type.getLimiteMonthlyDefault(),
                    "cartesExistantes", existingCards.size(),
                    "maxCartes", 5,
                    "canCreate", existingCards.size() < 5,
                    "restrictions",
                    type == CarteType.VIRTUELLE_GRATUITE ? "Une seule carte gratuite autorisée" : "Aucune restriction");

            return ResponseEntity.ok(simulation);

        } catch (Exception e) {
            log.error("❌ Erreur simulation création: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ========================================
    // ENDPOINTS ADMINISTRATIFS
    // ========================================

    /**
     * Lister toutes les cartes (Admin)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister toutes les cartes (Admin)")
    public ResponseEntity<List<Carte>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        try {
            // Implementation simplifiée - en réalité utiliser Pageable
            List<Carte> cartes = carteService.getAllCardsForAdmin(page, size);
            return ResponseEntity.ok(cartes);

        } catch (Exception e) {
            log.error("❌ Erreur récupération toutes cartes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Bloquer/débloquer une carte (Admin)
     */
    @PutMapping("/admin/{idCarte}/admin-block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bloquer une carte (Admin)")
    public ResponseEntity<CarteOperationResult> adminBlockCard(
            @PathVariable @NotBlank String idCarte,
            @RequestParam String reason,
            Authentication authentication) {

        try {
            String adminId = extractClientId(authentication);

            CarteOperationResult result = carteService.blockCard(idCarte, reason, adminId);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("❌ Erreur blocage admin: {}", e.getMessage(), e);
            CarteOperationResult errorResult = CarteOperationResult.failed("ADMIN_BLOCK", "ERREUR_TECHNIQUE",
                    "Erreur technique");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Santé du service
     */
    @GetMapping("/health")
    @Operation(summary = "Vérifier la santé du service")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "CarteService",
                "version", "1.0.0",
                "timestamp", java.time.LocalDateTime.now());

        return ResponseEntity.ok(health);
    }

    // ========================================
    // MÉTHODES UTILITAIRES
    // ========================================

    private String extractClientId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) authentication
                    .getPrincipal();
            return userDetails.getUsername();
        }
        throw new SecurityException("Client non authentifié");
    }

    private HttpStatus getHttpStatusFromError(String errorCode) {
        return switch (errorCode) {
            case "SOLDE_INSUFFISANT", "SOLDE_INSUFFISANT_CARTE" -> HttpStatus.PAYMENT_REQUIRED;
            case "CARTE_NON_AUTORISEE", "COMPTE_INTROUVABLE" -> HttpStatus.FORBIDDEN;
            case "CARTE_INTROUVABLE", "COMPTE_INACTIF" -> HttpStatus.NOT_FOUND;
            case "LIMITE_CARTE_GRATUITE", "LIMITE_CARTES_DEPASSEE" -> HttpStatus.CONFLICT;
            case "CARTE_INACTIVE", "CARTE_EXPIREE", "PIN_INCORRECT" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    /*
     * recharge d'une carte de credit par l'api money service
     */
    @PostMapping("/{idCarte}/recharge-orange-money")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RechargeResult> rechargeFromOrangeMoney(
            @PathVariable String idCarte,
            @RequestBody @Valid OrangeMoneyRechargeRequest request,
            Authentication authentication) {

        try {
            log.info("💳 [RECHARGE] Demande recharge Orange Money - Carte: {}, Montant: {}",
                    idCarte, request.getMontant());

            String clientId = authentication.getName();

            // Vérifier que la carte appartient au client
            Carte carte = carteService.findById(idCarte);
            if (carte == null || !carte.getIdClient().equals(clientId)) {
                return ResponseEntity.badRequest().body(
                        RechargeResult.failed("Carte non trouvée ou non autorisée"));
            }

            // Vérifier que la carte est active
            if (!carte.isActive()) {
                return ResponseEntity.badRequest().body(
                        RechargeResult.failed("Carte non active"));
            }

            // Appeler le service Money
            Map<String, Object> moneyResponse = moneyServiceClient.initiateCardRecharge(idCarte, request, clientId);

            String status = (String) moneyResponse.get("status");
            String message = (String) moneyResponse.get("message");
            String requestId = (String) moneyResponse.get("requestId");

            if ("PENDING".equals(status)) {
                return ResponseEntity.ok(RechargeResult.success(requestId, request.getMontant(), message));
            } else {
                return ResponseEntity.badRequest().body(RechargeResult.failed(message));
            }

        } catch (Exception e) {
            log.error("❌ [RECHARGE] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    RechargeResult.failed("Erreur technique: " + e.getMessage()));
        }
    }


    /*
     * recharge d'une carte de credit par l'api money service
     */
    @PostMapping("/{idCarte}/debit-orange-money")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RechargeResult> debitFromOrangeMoney(
            @PathVariable String idCarte,
            @RequestBody @Valid OrangeMoneyRechargeRequest request,
            Authentication authentication) {

        try {
            log.info("💳 [RECHARGE] Demande recharge Orange Money - Carte: {}, Montant: {}",
                    idCarte, request.getMontant());

            String clientId = authentication.getName();

            // Vérifier que la carte appartient au client
            Carte carte = carteService.findById(idCarte);
            if (carte == null || !carte.getIdClient().equals(clientId)) {
                return ResponseEntity.badRequest().body(
                        RechargeResult.failed("Carte non trouvée ou non autorisée"));
            }

            // Vérifier que la carte est active
            if (!carte.isActive()) {
                return ResponseEntity.badRequest().body(
                        RechargeResult.failed("Carte non active"));
            }

            // Appeler le service Money
            Map<String, Object> moneyResponse = moneyServiceClient.initiateCardRecharge(idCarte, request, clientId);

            String status = (String) moneyResponse.get("status");
            String message = (String) moneyResponse.get("message");
            String requestId = (String) moneyResponse.get("requestId");

            if ("PENDING".equals(status)) {
                return ResponseEntity.ok(RechargeResult.success(requestId, request.getMontant(), message));
            } else {
                return ResponseEntity.badRequest().body(RechargeResult.failed(message));
            }

        } catch (Exception e) {
            log.error("❌ [RECHARGE] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    RechargeResult.failed("Erreur technique: " + e.getMessage()));
        }
    }

    // ========================================
    // GESTION D'ERREURS
    // ========================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = Map.of(
                "error", "INVALID_ARGUMENT",
                "message", ex.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException ex) {
        Map<String, String> error = Map.of(
                "error", "SECURITY_ERROR",
                "message", "Accès non autorisé",
                "timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Erreur runtime dans contrôleur carte: {}", ex.getMessage(), ex);

        Map<String, String> error = Map.of(
                "error", "RUNTIME_ERROR",
                "message", "Erreur technique",
                "timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}