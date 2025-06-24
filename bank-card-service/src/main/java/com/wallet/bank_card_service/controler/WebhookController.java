package com.wallet.bank_card_service.controler;

import com.wallet.bank_card_service.service.CarteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final CarteService carteService;

    @PostMapping("/money-callback")
    public ResponseEntity<Void> handleMoneyCallback(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-Source-Service") String sourceService) {

        try {
            String status = (String) payload.get("status");
            String clientAction = (String) payload.get("clientAction");
            String cancellationReason = (String) payload.get("cancellationReason");
            String idCarte = (String) payload.get("idCarte");
            BigDecimal montant = new BigDecimal(payload.get("montant").toString());

            log.info("📨 [WEBHOOK] Callback - Carte: {}, Status: {}, Action: {}",
                    idCarte, status, clientAction);

            switch (status) {
                case "SUCCESS":
                    if ("VALIDATED".equals(clientAction)) {
                        carteService.creditCarteFromOrangeMoney(idCarte, montant,
                                (String) payload.get("transactionId"));
                        log.info("✅ Client a validé → Carte créditée");
                    }
                    break;

                case "CANCELLED":
                    log.info("❌ Client a annulé le paiement - Raison: {}", cancellationReason);
                    // Pas de crédit, juste log
                    break;

                case "EXPIRED":
                    log.warn("⏰ Paiement expiré - Client n'a pas validé");
                    break;

                case "INSUFFICIENT_FUNDS":
                    log.warn("💸 Solde Orange Money insuffisant");
                    break;

                default:
                    log.error("🔧 Erreur technique - Status: {}", status);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ [WEBHOOK] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
 /**
     * NOUVELLE MÉTHODE: Callback résultat retrait
     */
    @PostMapping("/money-withdrawal-callback")
    public ResponseEntity<Void> handleMoneyWithdrawalCallback(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-Source-Service") String sourceService,
            @RequestHeader(value = "X-Callback-Type", required = false) String callbackType) {

        try {
            String status = (String) payload.get("status");
            String clientAction = (String) payload.get("clientAction");
            String cancellationReason = (String) payload.get("cancellationReason");
            String idCarte = (String) payload.get("idCarte");
            String requestId = (String) payload.get("requestId");
            String transactionId = (String) payload.get("transactionId");

            log.info("📨 [WITHDRAWAL-WEBHOOK] Callback retrait - Carte: {}, Status: {}, Action: {}",
                    idCarte, status, clientAction);

            switch (status) {
                case "SUCCESS":
                    if ("COMPLETED".equals(clientAction)) {
                        // Retrait réussi - Rien à faire côté carte (déjà débitée)
                        log.info("✅ Retrait confirmé réussi - Carte: {}, Transaction: {}", 
                                idCarte, transactionId);
                        
                        // Optionnel: notification client
                        carteService.notifyClientWithdrawalSuccess(idCarte, requestId);
                    }
                    break;

                case "FAILED":
                    if ("FAILED".equals(clientAction)) {
                        log.warn("❌ Retrait échoué - Carte: {}, Raison: {}", idCarte, cancellationReason);
                        
                        // Optionnel: notification client de l'échec
                        carteService.notifyClientWithdrawalFailure(idCarte, requestId, cancellationReason);
                    }
                    break;

                default:
                    log.warn("⚠️ Statut retrait inconnu: {} - Carte: {}", status, idCarte);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ [WITHDRAWAL-WEBHOOK] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * NOUVELLE MÉTHODE: Callback remboursement retrait échoué
     */
    @PostMapping("/money-withdrawal-refund")
    public ResponseEntity<Void> handleMoneyWithdrawalRefund(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-Source-Service") String sourceService,
            @RequestHeader(value = "X-Callback-Type", required = false) String callbackType) {

        try {
            String status = (String) payload.get("status");
            String clientAction = (String) payload.get("clientAction");
            String cancellationReason = (String) payload.get("cancellationReason");
            String idCarte = (String) payload.get("idCarte");
            String requestId = (String) payload.get("requestId");
            BigDecimal montant = new BigDecimal(payload.get("montant").toString());

            log.info("💰 [REFUND-WEBHOOK] Demande remboursement - Carte: {}, Montant: {}, Raison: {}",
                    idCarte, montant, cancellationReason);

            if ("REFUND_REQUIRED".equals(status) && "REFUND".equals(clientAction)) {
                
                // Calculer les frais qui avaient été débités
                BigDecimal fraisEstimes = calculateWithdrawalFees(montant);
                
                // Rembourser la carte (montant + frais)
                carteService.refundCardWithdrawal(idCarte, montant, fraisEstimes, cancellationReason);
                
                log.info("✅ Remboursement effectué - Carte: {}, Montant total: {}", 
                        idCarte, montant.add(fraisEstimes));
                
                // Notification client du remboursement
                carteService.notifyClientWithdrawalRefund(idCarte, requestId, montant.add(fraisEstimes));
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ [REFUND-WEBHOOK] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Méthode utilitaire pour calculer les frais (même logique que dans le contrôleur)
    private BigDecimal calculateWithdrawalFees(BigDecimal montant) {
        BigDecimal frais = montant.multiply(new BigDecimal("0.01"));
        
        if (frais.compareTo(new BigDecimal("100")) < 0) {
            frais = new BigDecimal("100");
        } else if (frais.compareTo(new BigDecimal("1000")) > 0) {
            frais = new BigDecimal("1000");
        }
        
        return frais;
    }
}