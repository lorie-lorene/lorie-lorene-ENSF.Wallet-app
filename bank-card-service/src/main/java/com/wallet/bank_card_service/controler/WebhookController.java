package com.wallet.bank_card_service.controler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.bank_card_service.service.CarteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cartes/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    @Autowired
    private CarteService carteService;

    /**
     * Webhook appelé par le service Money quand une recharge est confirmée
     */
    @PostMapping("/money-callback")
    public ResponseEntity<Map<String, Object>> handleMoneyCallback(@RequestBody String rawPayload) {
        log.info("🔔 [WEBHOOK] Callback reçu du service Money: {}", rawPayload);

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(rawPayload, Map.class);

            String requestId = (String) payload.get("requestId");
            String idCarte = (String) payload.get("idCarte");
            String status = (String) payload.get("status");
            Object montantObj = payload.get("montant");

            if (requestId == null || idCarte == null || status == null) {
                log.warn("⚠️ [WEBHOOK] Payload invalide: {}", rawPayload);
                Map<String, Object> errorResponse = Map.of(
                        "status", "error",
                        "message", "Payload invalide");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Convertir le montant
            BigDecimal montant = new BigDecimal(montantObj.toString());

            log.info("📝 [WEBHOOK] Transaction - RequestId: {}, Carte: {}, Status: {}, Montant: {}",
                    requestId, idCarte, status, montant);

            if ("SUCCESS".equals(status)) {
                // ✅ Créditer la carte
                carteService.crediterCarte(idCarte, montant, requestId);
                log.info("✅ [WEBHOOK] Carte créditée avec succès - Carte: {}, Montant: {}", idCarte, montant);

                // Retourner une réponse JSON de succès
                Map<String, Object> successResponse = Map.of(
                        "status", "success",
                        "message", "Webhook traité avec succès",
                        "requestId", requestId,
                        "cardId", idCarte,
                        "amount", montant);
                return ResponseEntity.ok(successResponse);
            } else {
                log.warn("❌ [WEBHOOK] Recharge échouée - Status: {}", status);
                Map<String, Object> failureResponse = Map.of(
                        "status", "failed",
                        "message", "Recharge échouée",
                        "requestId", requestId,
                        "originalStatus", status);
                return ResponseEntity.ok(failureResponse);
            }

        } catch (Exception e) {
            log.error("❌ [WEBHOOK] Erreur traitement: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = Map.of(
                    "status", "error",
                    "message", "Erreur traitement webhook",
                    "error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Webhook pour les retraits carte
     */
    @PostMapping("/money-withdrawal-callback")
    public ResponseEntity<Void> handleWithdrawalCallback(@RequestBody Map<String, Object> payload) {
        try {
            log.info("📨 [WITHDRAWAL-CALLBACK] Reçu du Money Service: {}", payload);

            String requestId = (String) payload.get("requestId");
            String idCarte = (String) payload.get("idCarte");
            String status = (String) payload.get("status");
            String clientAction = (String) payload.get("clientAction");

            // Montants pour gestion remboursement
            BigDecimal montant = payload.get("montant") != null ? new BigDecimal(payload.get("montant").toString())
                    : BigDecimal.ZERO;

            if ("SUCCESS".equals(status) && "COMPLETED".equals(clientAction)) {
                // Retrait réussi - Notifier le client
                carteService.notifyClientWithdrawalSuccess(idCarte, requestId);

                log.info("✅ [WITHDRAWAL-CALLBACK] Retrait confirmé réussi - RequestId: {}", requestId);

            } else if ("FAILED".equals(status) || "REFUND_REQUIRED".equals(status)) {
                // Retrait échoué - Rembourser la carte
                String reason = (String) payload.get("cancellationReason");
                handleWithdrawalFailure(idCarte, requestId, montant, reason);

            } else {
                log.warn("⚠️ [WITHDRAWAL-CALLBACK] Statut inconnu: {} pour RequestId: {}",
                        status, requestId);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ [WITHDRAWAL-CALLBACK] Erreur traitement callback retrait: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * NOUVEAU CALLBACK: Pour les demandes de remboursement
     */
    @PostMapping("/money-withdrawal-refund")
    public ResponseEntity<Void> handleWithdrawalRefund(@RequestBody Map<String, Object> payload) {
        try {
            log.info("💰 [REFUND-CALLBACK] Demande remboursement reçue: {}", payload);

            String requestId = (String) payload.get("requestId");
            String idCarte = (String) payload.get("idCarte");
            BigDecimal montant = new BigDecimal(payload.get("montant").toString());
            String reason = (String) payload.get("cancellationReason");

            // Effectuer le remboursement
            handleWithdrawalFailure(idCarte, requestId, montant, reason);

            log.info("✅ [REFUND-CALLBACK] Remboursement traité - RequestId: {}", requestId);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ [REFUND-CALLBACK] Erreur traitement remboursement: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Gérer l'échec de retrait et rembourser
     */
    private void handleWithdrawalFailure(String idCarte, String requestId, BigDecimal montant, String reason) {
        try {
            // Calculer frais remboursés (même calcul que lors du débit)
            BigDecimal frais = calculateWithdrawalFees(montant);

            // Rembourser la carte (montant + frais)
            carteService.refundCardWithdrawal(idCarte, montant, frais, reason);

            // Notifier le client de l'échec et du remboursement
            carteService.notifyClientWithdrawalFailure(idCarte, requestId, reason);
            carteService.notifyClientWithdrawalRefund(idCarte, requestId, montant.add(frais));

            log.info("💰 [REFUND] Carte remboursée - ID: {}, Montant: {}, Frais: {}",
                    idCarte, montant, frais);

        } catch (Exception e) {
            log.error("❌ [REFUND] Erreur remboursement carte {}: {}", idCarte, e.getMessage(), e);
        }
    }

    /**
     * Calculer les frais de retrait (même logique que dans CarteController)
     */
    private BigDecimal calculateWithdrawalFees(BigDecimal montant) {
        BigDecimal frais = montant.multiply(new BigDecimal("0.01")); // 1%

        if (frais.compareTo(new BigDecimal("100")) < 0) {
            frais = new BigDecimal("100");
        } else if (frais.compareTo(new BigDecimal("1000")) > 0) {
            frais = new BigDecimal("1000");
        }

        return frais;
    }

    /**
     * Endpoint de test pour vérifier les callbacks
     */
    @PostMapping("/test-callback")
    public ResponseEntity<Map<String, Object>> testCallback(@RequestBody Map<String, Object> payload) {
        log.info("🔧 [TEST-CALLBACK] Test callback reçu: {}", payload);

        return ResponseEntity.ok(Map.of(
                "received", true,
                "timestamp", LocalDateTime.now(),
                "payload", payload));
    }

    /**
     * Statut du service de callbacks
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCallbackStatus() {
        return ResponseEntity.ok(Map.of(
                "service", "Carte Callback Service",
                "status", "UP",
                "endpoints", Map.of(
                        "recharge", "/money-callback",
                        "withdrawal", "/money-withdrawal-callback",
                        "refund", "/money-withdrawal-refund"),
                "timestamp", LocalDateTime.now()));
    }
}