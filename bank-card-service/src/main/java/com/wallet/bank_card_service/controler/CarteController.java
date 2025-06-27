package com.wallet.bank_card_service.controler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
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
import com.wallet.bank_card_service.dto.CarteWithdrawalRequest;
import com.wallet.bank_card_service.dto.CarteWithdrawalResult;
import com.wallet.bank_card_service.dto.OrangeMoneyRechargeRequest;
import com.wallet.bank_card_service.dto.PinChangeRequest;
import com.wallet.bank_card_service.dto.RechargeResult;
import com.wallet.bank_card_service.dto.TransferCardToCardRequest;
import com.wallet.bank_card_service.dto.TransfertCarteRequest;
import com.wallet.bank_card_service.dto.TransfertCarteResult;
import com.wallet.bank_card_service.model.Carte;
import com.wallet.bank_card_service.service.AgenceServiceClient;
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
    @Autowired
    private AgenceServiceClient agenceServiceClient;

    /**
     * Créer une nouvelle carte bancaire ok!
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

            log.info("🆕 Demande création carte: client={},agence={} type={}", clientId, request.getIdAgence(),
                    request.getType());

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
     * Lister toutes les cartes d'un client ok!
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
     * Détails d'une carte spécifique ok!
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
     * Transfert d'argent du compte vers une carte ok!
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
        log.info("💳 Transfert vers carte1: compte={}, carte={}, montant={},agence={}",
                request.getNumeroCompteSource(), request.getIdCarteDestination(), request.getMontant(),
                request.getIdAgence());

        try {
            String clientId = extractClientId(authentication);

            log.info("💳 Transfert vers carte: compte={}, carte={}, montant={},agence={}",
                    request.getNumeroCompteSource(), request.getIdCarteDestination(), request.getMontant(),
                    request.getIdAgence());

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
     * Bloquer une carte ok!
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
     * Débloquer une carte ok!
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
     * Modifier les paramètres d'une carte ok !
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
     * Changer le code PIN d'une carte ok!
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
            @RequestBody @Valid TransferCardToCardRequest request,
            Authentication authentication) {

        try {
            String clientId = "1";

            log.info("💳➡️💳 Transfert carte à carte: {} -> {}, montant={}",
                    request.getNumeroCompteSource(), request.getIdCarteDestination(), request.getMontant());

            TransfertCarteResult debitResult = carteService.transferFromCard(
                    request.getNumeroCompteSource(),
                    request.getMontant(),
                    "Transfert vers carte " + request.getIdCarteDestination(),
                    clientId);

            if (!debitResult.isSuccess()) {
                return ResponseEntity.badRequest().body(debitResult);
            }

            // Récupérer les détails de la carte destination pour avoir son compte
            Carte carteDestination = carteService.getCardDetails(request.getIdCarteDestination(), clientId);

            TransfertCarteRequest creditRequest = new TransfertCarteRequest();
            creditRequest.setNumeroCompteSource(carteDestination.getNumeroCompte());
            creditRequest.setIdCarteDestination(request.getIdCarteDestination());
            creditRequest.setMontant(request.getMontant());
            creditRequest.setDescription(request.getDescription() != null ? request.getDescription()
                    : "Transfert depuis carte " + request.getNumeroCompteSource());

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
     * Historique des transactions d'une carte ok!
     */
    @GetMapping("/{idCarte}/transactions")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Historique des transactions d'une carte")
    public ResponseEntity<List<Carte.CarteAction>> getCardTransactions(
            @PathVariable @NotBlank String idCarte,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {

        try {
            String clientId = "extractClientId(authentication)";

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
     * Configuration des frais par type de carte (Info publique) ok!
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
     * Lister toutes les cartes (Admin) ok!
     */

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister toutes les cartes (Admin)")
    public ResponseEntity<?> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        try {
            log.info("📋 Récupération de toutes les cartes - Page: {}, Size: {}", page, size);

            // Validation des paramètres
            if (page < 0 || size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Paramètres invalides",
                                "message", "page >= 0 et 0 < size <= 100"));
            }

            List<Carte> cartes = carteService.getAllCardsForAdmin(page, size);

            log.info("✅ {} cartes récupérées avec succès", cartes.size());
            return ResponseEntity.ok(Map.of(
                    "data", cartes,
                    "page", page,
                    "size", size,
                    "total", cartes.size()));

        } catch (Exception e) {
            log.error("❌ Erreur récupération toutes cartes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne",
                            "message", "Impossible de récupérer les cartes"));
        }
    }

    /**
     * Bloquer/débloquer une carte (Admin) ok!
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
            // String clientId = "1";
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

    /**
     * NOUVELLE MÉTHODE: Retrait depuis carte vers Orange/MTN Money utilisation de
     * raabitmq
     */
    @PostMapping("/{idCarte}/withdraw-to-mobile-money")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Retirer de l'argent d'une carte vers Orange/MTN Money")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retrait initié avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "402", description = "Solde carte insuffisant"),
            @ApiResponse(responseCode = "403", description = "Carte non autorisée ou PIN incorrect")
    })
    public ResponseEntity<CarteWithdrawalResult> withdrawToMobileMoney(
            @PathVariable String idCarte,
            @RequestBody @Valid CarteWithdrawalRequest request,
            Authentication authentication) {

        try {
            log.info("💸 [WITHDRAWAL] Demande retrait - Carte: {}, Montant: {}, Provider: {}",
                    idCarte, request.getMontant(), request.getProvider());

            String clientId = authentication.getName();
            // String clientId = "1";

            // 1. Vérifier que la carte appartient au client et est active
            Carte carte = carteService.findById(idCarte);
            if (carte == null || !carte.getIdClient().equals(clientId)) {
                return ResponseEntity.badRequest().body(
                        CarteWithdrawalResult.failed("Carte non trouvée ou non autorisée"));
            }

            if (!carte.isActive()) {
                return ResponseEntity.badRequest().body(
                        CarteWithdrawalResult.failed("Carte non active"));
            }

            // 2. Vérifier le PIN de la carte
            if (!carteService.verifyCardPin(idCarte, request.getCodePin())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        CarteWithdrawalResult.failed("Code PIN incorrect"));
            }

            // 3. Vérifier limites de retrait quotidien
            if (!carteService.canWithdraw(idCarte, request.getMontant())) {
                return ResponseEntity.badRequest().body(
                        CarteWithdrawalResult.failed("Limite de retrait quotidien atteinte"));
            }

            // 4. Vérifier solde carte suffisant (inclus les frais)
            BigDecimal fraisEstimes = calculateWithdrawalFees(request.getMontant());
            BigDecimal montantTotal = request.getMontant().add(fraisEstimes);

            if (carte.getSolde().compareTo(montantTotal) < 0) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(
                        CarteWithdrawalResult.failed("Solde insuffisant. Requis: " + montantTotal + " FCFA"));
            }

            // 5. Appeler le service Money pour initier le retrait
            Map<String, Object> moneyResponse = moneyServiceClient.initiateCardWithdrawal(idCarte, request, clientId);

            String status = (String) moneyResponse.get("status");
            String message = (String) moneyResponse.get("message");
            String requestId = (String) moneyResponse.get("reference");

            if ("SUCCESS".equals(status) || "PENDING".equals(status)) {
                // 6. Débiter immédiatement la carte (le Money Service gère les remboursements
                // en cas d'échec)
                carteService.debitCarteForWithdrawal(idCarte, request.getMontant(), fraisEstimes, requestId);

                return ResponseEntity.ok(CarteWithdrawalResult.success(
                        requestId, idCarte, request.getMontant(), fraisEstimes,
                        carte.getSolde().subtract(montantTotal), request.getProvider(), message));
            } else {
                return ResponseEntity.badRequest().body(CarteWithdrawalResult.failed(message));
            }

        } catch (Exception e) {
            log.error("❌ [WITHDRAWAL] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    CarteWithdrawalResult.failed("Erreur technique: " + e.getMessage()));
        }
    }

    /**
     * Historique des retraits d'une carte
     */
    @GetMapping("/{idCarte}/withdrawal-history")
    // @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Historique des retraits d'une carte")
    public ResponseEntity<List<Carte.CarteAction>> getCardWithdrawalHistory(
            @PathVariable @NotBlank String idCarte,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        try {
            // String clientId = extractClientId(authentication);
            String clientId = "1";
            Carte carte = carteService.getCardDetails(idCarte, clientId);

            List<Carte.CarteAction> withdrawals = carte.getActionsHistory()
                    .stream()
                    .filter(action -> action.getType() == Carte.CarteActionType.DEBIT &&
                            action.getDescription().contains("Retrait"))
                    .limit(limit)
                    .toList();

            return ResponseEntity.ok(withdrawals);

        } catch (Exception e) {
            log.error("❌ Erreur récupération historique retraits: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Méthode utilitaire pour calculer les frais
    private BigDecimal calculateWithdrawalFees(BigDecimal montant) {
        BigDecimal frais = montant.multiply(new BigDecimal("0.01"));

        if (frais.compareTo(new BigDecimal("100")) < 0) {
            frais = new BigDecimal("100");
        } else if (frais.compareTo(new BigDecimal("1000")) > 0) {
            frais = new BigDecimal("1000");
        }

        return frais;
    }

    /**
     * Endpoint de santé du service carte
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Test connectivité service agence
            boolean agenceConnected = agenceServiceClient.testConnection();

            health.put("status", "UP");
            health.put("service", "BankCardService");
            health.put("version", "1.0.0");
            health.put("timestamp", LocalDateTime.now());
            health.put("dependencies", Map.of(
                    "agenceService", agenceConnected ? "UP" : "DOWN"));

            if (!agenceConnected) {
                health.put("status", "DEGRADED");
                health.put("warnings", "Service agence inaccessible");
            }

        } catch (Exception e) {
            log.error("❌ Erreur health check: {}", e.getMessage());
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }

        return ResponseEntity.ok(health);
    }

    /**
     * Test de connectivité avec tous les services externes
     */
    @GetMapping("/connectivity-test")
    public ResponseEntity<Map<String, Object>> testConnectivity() {
        log.info("🔧 Test de connectivité depuis service carte vers service agence");

        Map<String, Object> result = new HashMap<>();

        try {
            // Test connectivité vers le service agence
            boolean agenceHealthOk = agenceServiceClient.testConnection2();
            boolean agenceTestOk = agenceServiceClient.testConnection2();

            Map<String, Object> agenceDetails = new HashMap<>();
            agenceDetails.put("healthEndpoint", agenceHealthOk ? "CONNECTED" : "DISCONNECTED");
            agenceDetails.put("testEndpoint", agenceTestOk ? "CONNECTED" : "DISCONNECTED");
            agenceDetails.put("targetUrl", "http://localhost:8095/api/money/card-recharge");
            agenceDetails.put("targetPort", 8095);

            result.put("timestamp", LocalDateTime.now());
            result.put("sourceService", "CarteService");
            result.put("sourcePort", 8096);
            result.put("targetService", "moneyService");
            result.put("services", Map.of("moneyservice", agenceDetails));

            String overallStatus;
            if (agenceHealthOk && agenceTestOk) {
                overallStatus = "ALL_CONNECTED";
            } else if (agenceHealthOk || agenceTestOk) {
                overallStatus = "PARTIAL_CONNECTIVITY";
            } else {
                overallStatus = "NO_CONNECTIVITY";
            }

            result.put("overall", overallStatus);

        } catch (Exception e) {
            log.error("❌ Erreur test connectivité: {}", e.getMessage());
            result.put("error", e.getMessage());
            result.put("overall", "CONNECTION_ERROR");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Informations sur les endpoints disponibles
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getEndpoints() {
        Map<String, Object> endpoints = new HashMap<>();

        endpoints.put("service", "BankCardService");
        endpoints.put("baseUrl", "/api/v1/cartes");
        endpoints.put("endpoints", Map.of(
                "POST /create", "Créer une nouvelle carte",
                "GET /my-cards", "Lister mes cartes",
                "GET /{id}", "Détails d'une carte",
                "POST /transfer-to-card", "Transférer vers carte",
                "POST /{id}/transfer-to-account", "Transférer depuis carte",
                "PUT /{id}/block", "Bloquer une carte",
                "PUT /{id}/unblock", "Débloquer une carte",
                "GET /health", "Santé du service",
                "GET /connectivity-test", "Test connectivité"));

        return ResponseEntity.ok(endpoints);
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