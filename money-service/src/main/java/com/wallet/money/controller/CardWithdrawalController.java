package com.wallet.money.controller;

import com.wallet.money.entity.*;
import com.wallet.money.service.RetraitMoneyService;
import com.wallet.money.service.TransactionService;
import com.wallet.money.service.CardServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/withdrawals")
@RequiredArgsConstructor
@Slf4j
public class CardWithdrawalController {

    @Autowired
    private RetraitMoneyService withdrawalService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CardServiceClient cardServiceClient;

    /**
     * ENDPOINT RETRAIT: Retrait depuis carte vers Mobile Money
     */
    @PostMapping("/from-card")
    public ResponseEntity<PaymentResponse> withdrawFromCard(
            @RequestBody CardWithdrawalRequest request,
            @RequestHeader(value = "X-Client-ID", required = false) String clientId,
            @RequestHeader(value = "X-Source-Service", required = false) String sourceService) {

        try {
            log.info("💸 [CARD-WITHDRAWAL] Demande reçue - Carte: {}, Montant: {}, Provider: {}",
                    request.getIdCarte(), request.getAmount(), request.getProvider());

            // 1. Validation des données
            if (request.getAmount() < 100) {
                return ResponseEntity.badRequest().body(createErrorResponse("Montant minimum 100 FCFA"));
            }

            if (request.getAmount() > 200000) {
                return ResponseEntity.badRequest().body(createErrorResponse("Montant maximum 200,000 FCFA"));
            }

            // 2. Créer transaction de type CARD_WITHDRAWAL avec callback URL spécifique
            Transaction transaction = createCardWithdrawalTransaction(clientId, request);

            // 3. Préparer requête pour RetraitMoneyService
            RetraitRequest retraitRequest = new RetraitRequest();
            retraitRequest.setReceiver(request.getReceiver());
            retraitRequest.setAmount(request.getAmount());
            retraitRequest.setDescription("Retrait carte " + request.getIdCarte() + " vers " + request.getProvider());
            retraitRequest.setExternalId(transaction.getExternalId());
            
            // ✅ CORRECTION: URL callback spécifique aux retraits carte
            retraitRequest.setCallback("http://localhost:8095/webhook/freemopay/card-withdrawal");

            // 4. Utiliser le service de retrait existant
            PaymentResponse response = withdrawalService.initiateWithdrawal(retraitRequest);

            // 5. Sauvegarder référence FreemoPay
            transactionService.updateFreemoReference(transaction.getId(), response.getReference());

            // 6. Adapter la réponse avec informations détaillées
            response.setMessage("Retrait initié depuis carte " + request.getIdCarte() + 
                    " vers " + request.getProvider() + ". " + response.getMessage());

            log.info("✅ [CARD-WITHDRAWAL] Retrait initié - ExternalId: {}, FreemoRef: {}, Status: {}",
                    transaction.getExternalId(), response.getReference(), response.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [CARD-WITHDRAWAL] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Erreur technique: " + e.getMessage()));
        }
    }

    /**
     * Vérifier le statut d'un retrait carte
     */
    @GetMapping("/card-withdrawal/status/{requestId}")
    public ResponseEntity<PaymentResponse> getCardWithdrawalStatus(@PathVariable String requestId) {
        try {
            Transaction transaction = transactionService.findByExternalId(requestId);

            if (transaction == null) {
                return ResponseEntity.notFound().build();
            }

            PaymentResponse response = new PaymentResponse();
            response.setReference(transaction.getFreemoReference());
            response.setStatus(transaction.getStatus());
            response.setMessage("Statut retrait carte: " + transaction.getStatus() + 
                               " - Carte: " + transaction.getIdCarte());

            log.info("📊 [CARD-WITHDRAWAL] Statut demandé - RequestId: {}, Status: {}", 
                     requestId, transaction.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [CARD-WITHDRAWAL] Erreur récupération statut: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse("Erreur récupération statut"));
        }
    }

    /**
     * Historique des retraits d'une carte
     */
    @GetMapping("/card-withdrawal/history/{idCarte}")
    public ResponseEntity<?> getCardWithdrawalHistory(
            @PathVariable String idCarte,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            // Récupérer les transactions de retrait pour cette carte
            List<Transaction> withdrawals = transactionService.getClientCardWithdrawals("", idCarte, limit);
            
            return ResponseEntity.ok(Map.of(
                "idCarte", idCarte,
                "withdrawals", withdrawals,
                "total", withdrawals.size(),
                "retrievedAt", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("❌ [CARD-WITHDRAWAL] Erreur historique: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Statistiques des retraits pour une carte
     */
    @GetMapping("/card-withdrawal/stats/{idCarte}")
    public ResponseEntity<?> getCardWithdrawalStats(@PathVariable String idCarte) {
        try {
            List<Transaction> withdrawals = transactionService.getClientCardWithdrawals("", idCarte, 100);
            
            BigDecimal totalAmount = withdrawals.stream()
                .map(Transaction::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long successfulCount = withdrawals.stream()
                .filter(t -> "SUCCESS".equals(t.getStatus()))
                .count();
            
            long failedCount = withdrawals.stream()
                .filter(t -> "FAILED".equals(t.getStatus()))
                .count();

            return ResponseEntity.ok(Map.of(
                "idCarte", idCarte,
                "totalWithdrawals", withdrawals.size(),
                "successfulWithdrawals", successfulCount,
                "failedWithdrawals", failedCount,
                "totalAmount", totalAmount,
                "successRate", withdrawals.size() > 0 ? (double) successfulCount / withdrawals.size() * 100 : 0,
                "calculatedAt", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("❌ [CARD-WITHDRAWAL] Erreur statistiques: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * NOUVELLE MÉTHODE: Test de connectivité avec le service carte
     */
    @GetMapping("/test-connectivity")
    public ResponseEntity<?> testConnectivity() {
        try {
            // Tester la communication avec le service carte
            boolean carteServiceUp = testCarteService();
            
            return ResponseEntity.ok(Map.of(
                "moneyService", "UP",
                "carteService", carteServiceUp ? "UP" : "DOWN",
                "timestamp", LocalDateTime.now(),
                "cardCallbackUrl", "http://localhost:8096/api/v1/cartes/webhooks/money-withdrawal-callback"
            ));

        } catch (Exception e) {
            log.error("❌ [CONNECTIVITY] Erreur test: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "moneyService", "UP",
                "carteService", "UNKNOWN",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }

    // ========================================
    // MÉTHODES PRIVÉES
    // ========================================

    private Transaction createCardWithdrawalTransaction(String clientId, CardWithdrawalRequest request) {
        Transaction transaction = new Transaction();
        transaction.setClientId(clientId != null ? clientId : "unknown");
        transaction.setExternalId("CARD_WIT_" + request.getIdCarte() + "_" + System.currentTimeMillis());
        transaction.setPhoneNumber(request.getReceiver());
        transaction.setAmount(BigDecimal.valueOf(request.getAmount()));
        transaction.setType("CARD_WITHDRAWAL");
        transaction.setStatus("PENDING");
        transaction.setIdCarte(request.getIdCarte());
        
        // ✅ CORRECTION: URL callback vers le service carte
        transaction.setCallbackUrl("http://localhost:8096/api/v1/cartes/webhooks/money-withdrawal-callback");
        
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        Transaction saved = transactionService.transactionRepository.save(transaction);

        log.info("💾 [CARD-WITHDRAWAL] Transaction créée - ExternalId: {}, CallbackUrl: {}", 
                 saved.getExternalId(), saved.getCallbackUrl());

        return saved;
    }

    private PaymentResponse createErrorResponse(String message) {
        PaymentResponse response = new PaymentResponse();
        response.setStatus("FAILED");
        response.setMessage(message);
        response.setReference(null);
        return response;
    }

    private boolean testCarteService() {
        try {
            // Test simple de ping vers le service carte
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://localhost:8096/api/v1/cartes/health";
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.warn("⚠️ [CONNECTIVITY] Service carte inaccessible: {}", e.getMessage());
            return false;
        }
    }
}