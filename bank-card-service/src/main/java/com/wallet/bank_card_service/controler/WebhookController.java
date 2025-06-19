package com.wallet.bank_card_service.controler;


import com.wallet.bank_card_service.service.CarteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
            log.info("📨 [WEBHOOK] Callback reçu du service Money - Payload: {}", payload);

            // Validation sécurité
            if (!"money-service".equals(sourceService)) {
                log.warn("⚠️ [WEBHOOK] Source non autorisée: {}", sourceService);
                return ResponseEntity.badRequest().build();
            }

            // Extraire données
            String requestId = (String) payload.get("requestId");
            String idCarte = (String) payload.get("idCarte");
            String status = (String) payload.get("status");
            BigDecimal montant = new BigDecimal(payload.get("montant").toString());
            String transactionId = (String) payload.get("transactionId");

            log.info("🔄 [WEBHOOK] Traitement - Carte: {}, Status: {}, Montant: {}", idCarte, status, montant);

            if ("SUCCESS".equals(status)) {
                // Créditer la carte
                carteService.creditCarteFromOrangeMoney(idCarte, montant, transactionId);
                log.info("✅ [WEBHOOK] Carte créditée avec succès");
            } else {
                log.warn("❌ [WEBHOOK] Recharge échouée - RequestId: {}", requestId);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ [WEBHOOK] Erreur traitement callback: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}