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
import com.wallet.money.service.TransactionService;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/webhook/freemopay")
public class WebhookController {

    @Autowired
    private TransactionService transactionService; // ⬅️ Changement ici

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
            String message = paymentResponse.getMessage(); // Pour la raison

            log.info("🔄 Webhook - Référence: {} | Statut: {}", reference, status);

            // REMPLACER l'ancien appel par le nouveau
            transactionService.updateStatusFromWebhookWithCardNotification(reference, status, message);

            log.info("✅ Webhook traité - Référence: {}", reference);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ Erreur webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * WEBHOOK SPÉCIFIQUE: Pour les retraits depuis carte vers Mobile Money
     */
    @PostMapping("/card-withdrawal")
    public ResponseEntity<Void> handleCardWithdrawalNotification(@RequestBody String rawBody) {
        log.info("📨 [CARD-WITHDRAWAL] Webhook retrait carte reçu - Raw payload = «{}»", rawBody);

        try {
            ObjectMapper mapper = new ObjectMapper();
            PaymentResponse paymentResponse = mapper.readValue(rawBody, PaymentResponse.class);

            if (paymentResponse == null || paymentResponse.getReference() == null) {
                log.warn("⚠️ [CARD-WITHDRAWAL] Webhook retrait carte invalide");
                return ResponseEntity.badRequest().build();
            }

            String reference = paymentResponse.getReference();
            String status = paymentResponse.getStatus();
            String message = paymentResponse.getMessage();

            log.info("🔄 [CARD-WITHDRAWAL] Webhook retrait carte - Référence: {} | Statut: {}", reference, status);

            // Traitement spécifique aux retraits carte
            transactionService.updateCardWithdrawalStatusFromWebhook(reference, status, message);

            log.info("✅ [CARD-WITHDRAWAL] Webhook retrait carte traité - Référence: {}", reference);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ [CARD-WITHDRAWAL] Erreur webhook retrait carte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * NOUVEAU WEBHOOK: Retour de statut spécifique Orange Money
     */
    @PostMapping("/orange-money-status")
    public ResponseEntity<Void> handleOrangeMoneyStatus(@RequestBody String rawBody) {
        log.info("📨 [ORANGE-MONEY] Webhook Orange Money reçu - Raw payload = «{}»", rawBody);

        try {
            ObjectMapper mapper = new ObjectMapper();
            PaymentResponse paymentResponse = mapper.readValue(rawBody, PaymentResponse.class);

            if (paymentResponse == null || paymentResponse.getReference() == null) {
                log.warn("⚠️ [ORANGE-MONEY] Webhook Orange Money invalide");
                return ResponseEntity.badRequest().build();
            }

            String reference = paymentResponse.getReference();
            String status = paymentResponse.getStatus();
            String message = paymentResponse.getMessage();

            log.info("🔄 [ORANGE-MONEY] Status Orange Money - Référence: {} | Statut: {}", reference, status);

            // Traitement unifié pour Orange Money
            transactionService.updateStatusFromWebhookWithCardNotification(reference, status, message);

            log.info("✅ [ORANGE-MONEY] Webhook Orange Money traité - Référence: {}", reference);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ [ORANGE-MONEY] Erreur webhook Orange Money: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * NOUVEAU WEBHOOK: Retour de statut spécifique MTN Money
     */
    @PostMapping("/mtn-money-status")
    public ResponseEntity<Void> handleMtnMoneyStatus(@RequestBody String rawBody) {
        log.info("📨 [MTN-MONEY] Webhook MTN Money reçu - Raw payload = «{}»", rawBody);

        try {
            ObjectMapper mapper = new ObjectMapper();
            PaymentResponse paymentResponse = mapper.readValue(rawBody, PaymentResponse.class);

            if (paymentResponse == null || paymentResponse.getReference() == null) {
                log.warn("⚠️ [MTN-MONEY] Webhook MTN Money invalide");
                return ResponseEntity.badRequest().build();
            }

            String reference = paymentResponse.getReference();
            String status = paymentResponse.getStatus();
            String message = paymentResponse.getMessage();

            log.info("🔄 [MTN-MONEY] Status MTN Money - Référence: {} | Statut: {}", reference, status);

            // Traitement unifié pour MTN Money
            transactionService.updateStatusFromWebhookWithCardNotification(reference, status, message);

            log.info("✅ [MTN-MONEY] Webhook MTN Money traité - Référence: {}", reference);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ [MTN-MONEY] Erreur webhook MTN Money: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Webhook de test pour débugger
     */
    @PostMapping("/test")
    public ResponseEntity<String> handleTestWebhook(@RequestBody String rawBody) {
        log.info("🔧 [TEST-WEBHOOK] Test webhook reçu: {}", rawBody);

        return ResponseEntity.ok("Test webhook reçu et traité avec succès. Timestamp: " +
                java.time.LocalDateTime.now());
    }
}