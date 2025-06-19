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

}