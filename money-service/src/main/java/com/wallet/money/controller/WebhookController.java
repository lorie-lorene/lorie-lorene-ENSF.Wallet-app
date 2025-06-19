package com.wallet.money.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.money.entity.PaymentResponse;
import com.wallet.money.service.CardServiceClient;
import com.wallet.money.service.TransactionService;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/webhook/freemopay")
public class WebhookController {

    @Autowired
    private TransactionService transactionService; // ⬅️ Changement ici
    @Autowired
    private CardServiceClient cardServiceClient;

    @PostMapping()
    public ResponseEntity<Void> handlePaymentNotification(@RequestBody String rawBody) {
        log.info("📨 Webhook FreemoPay reçu - Raw payload = «{}»", rawBody);

        try {
            ObjectMapper mapper = new ObjectMapper();
            PaymentResponse paymentResponse = mapper.readValue(rawBody, PaymentResponse.class);

            if (paymentResponse == null || paymentResponse.getReference() == null) {
                log.warn("⚠️ Webhook invalide");
                return ResponseEntity.badRequest().build();
            }

            String reference = paymentResponse.getReference();
            String status = paymentResponse.getStatus();

            log.info("🔄 Webhook - Référence: {} | Statut: {}", reference, status);

            // ⭐ REMPLACER l'ancienne méthode par la nouvelle
            transactionService.updateStatusFromWebhookWithCardNotification(reference, status, cardServiceClient);

            log.info("✅ Webhook traité - Référence: {}", reference);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ Erreur webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}