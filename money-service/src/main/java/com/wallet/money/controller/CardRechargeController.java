package com.wallet.money.controller;

import com.wallet.money.carteclient.CardRechargeRequest;
import com.wallet.money.carteclient.CardRechargeResponse;
//import com.wallet.money.dto.CardRechargeHistoryDTO;
import com.wallet.money.entity.PaymentRequest;
import com.wallet.money.entity.PaymentResponse;
import com.wallet.money.entity.Transaction;
import com.wallet.money.service.DepotMoneyService;
import com.wallet.money.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/money/card-recharge")
@RequiredArgsConstructor
@Slf4j
public class CardRechargeController {

    @Autowired
    private DepotMoneyService depotMoneyService;

    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<CardRechargeResponse> initiateCardRecharge(
            @RequestBody CardRechargeRequest request,
            @RequestHeader(value = "X-Client-ID", required = false) String clientId,
            @RequestHeader(value = "X-Source-Service", required = false) String sourceService) {

        log.info("💳 [CARD-RECHARGE] Demande reçue - Carte: {}, Montant: {}",
                request.getIdCarte(), request.getMontant());

        try {
            // Validation
            if (request.getMontant().compareTo(new BigDecimal("50")) < 0) {
                return ResponseEntity.badRequest().body(createErrorResponse(request, "Montant minimum 500 FCFA"));
            }

            // Créer transaction
            Transaction transaction = createCardRechargeTransaction(request, clientId);
            transaction = transactionService.transactionRepository.save(transaction);

            // Appeler FreemoPay
            PaymentRequest paymentRequest = new PaymentRequest(
                    request.getNumeroOrangeMoney(),
                    request.getMontant().doubleValue(),
                    transaction.getExternalId(),
                    "Recharge carte " + request.getIdCarte(),
                    "http://localhost:8095/webhook/freemopay");

            PaymentResponse freemoResponse = depotMoneyService.createPayment2(paymentRequest);
            transactionService.updateFreemoReference(transaction.getId(), freemoResponse.getReference());

            // Préparer réponse
            CardRechargeResponse response = new CardRechargeResponse();
            response.setRequestId(transaction.getExternalId());
            response.setIdCarte(request.getIdCarte());
            response.setMontant(request.getMontant());
            response.setFreemoReference(freemoResponse.getReference());
            response.setTimestamp(LocalDateTime.now());

            if ("SUCCESS".equals(freemoResponse.getStatus()) || "SUCCES".equals(freemoResponse.getStatus())) {

                // ✅ NOUVEAU : Déclencher immédiatement le callback si SUCCESS
                log.info("🚀 [IMMEDIATE-SUCCESS] FreemoPay retourné SUCCESS immédiatement - Déclenchement callback");

                try {
                    // Simuler la mise à jour comme si c'était un webhook
                    transactionService.updateStatusFromWebhookWithCardNotification(
                            freemoResponse.getReference(),
                            "SUCCESS",
                            "Recharge confirmée immédiatement");

                    response.setStatus("SUCCESS");
                    response.setMessage("Recharge confirmée et carte créditée !");

                } catch (Exception callbackError) {
                    log.error("❌ Erreur callback immédiat: {}", callbackError.getMessage());
                    response.setStatus("PENDING");
                    response.setMessage("Recharge en cours. Validation des callbacks...");
                }

            } else if ("PENDING".equals(freemoResponse.getStatus())) {
                response.setStatus("PENDING");
                response.setMessage("Recharge en cours. Validez sur votre téléphone Orange Money.");
            } else {
                response.setStatus("FAILED");
                response.setMessage("Erreur: " + freemoResponse.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [CARD-RECHARGE] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(request, "Erreur technique"));
        }
    }

    @GetMapping("/status/{requestId}")
    public ResponseEntity<CardRechargeResponse> getRechargeStatus(@PathVariable String requestId) {
        Transaction transaction = transactionService.findByExternalId(requestId);

        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        CardRechargeResponse response = new CardRechargeResponse();
        response.setRequestId(requestId);
        response.setStatus(transaction.getStatus());
        response.setTimestamp(LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    private Transaction createCardRechargeTransaction(CardRechargeRequest request, String clientId) {
        Transaction transaction = new Transaction();
        transaction.setClientId(clientId != null ? clientId : "unknown");
        transaction.setExternalId(request.getIdCarte() + "_" + System.currentTimeMillis());
        transaction.setPhoneNumber(request.getNumeroOrangeMoney());
        transaction.setAmount(request.getMontant());
        transaction.setType("CARD_RECHARGE");
        transaction.setStatus("PENDING");
        transaction.setIdCarte(request.getIdCarte());
        transaction.setCallbackUrl(request.getCallbackUrl());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        return transaction;
    }

    private CardRechargeResponse createErrorResponse(CardRechargeRequest request, String message) {
        CardRechargeResponse response = new CardRechargeResponse();
        response.setIdCarte(request.getIdCarte());
        response.setMontant(request.getMontant());
        response.setStatus("FAILED");
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setExternalId(transaction.getExternalId());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setStatus(transaction.getStatus());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());
        dto.setClientAction(transaction.getClientAction());
        dto.setCancellationReason(transaction.getCancellationReason());
        dto.setValidationTimestamp(transaction.getValidationTimestamp());

        // Masquer le numéro complet pour la sécurité
        if (transaction.getPhoneNumber() != null) {
            String phone = transaction.getPhoneNumber();
            dto.setPhoneNumberMasked(phone.substring(0, 6) + "***" + phone.substring(9));
        }

        // Infos carte si applicable
        if (transaction.getIdCarte() != null) {
            dto.setIdCarte(transaction.getIdCarte());
            // Vous pourriez ajouter le numéro masqué de la carte ici
        }

        return dto;
    }

    private CardRechargeHistoryDTO convertToCardRechargeDTO(Transaction transaction) {
        CardRechargeHistoryDTO dto = new CardRechargeHistoryDTO();
        dto.setRequestId(transaction.getExternalId());
        dto.setIdCarte(transaction.getIdCarte());
        dto.setMontant(transaction.getAmount());
        dto.setStatus(transaction.getStatus());
        dto.setClientAction(transaction.getClientAction());
        dto.setTimestamp(transaction.getCreatedAt());
        dto.setValidationTimestamp(transaction.getValidationTimestamp());
        dto.setCancellationReason(transaction.getCancellationReason());

        // Numéro Orange Money masqué
        if (transaction.getPhoneNumber() != null) {
            String phone = transaction.getPhoneNumber();
            dto.setNumeroOrangeMoneyMasked(phone.substring(0, 6) + "***" + phone.substring(9));
        }

        return dto;
    }

    public static class TransactionDTO {
        private String externalId;
        private BigDecimal amount;
        private String type;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String phoneNumberMasked;
        private String clientAction;
        private String cancellationReason;
        private LocalDateTime validationTimestamp;
        private String idCarte;

        // Getters et setters
        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getPhoneNumberMasked() {
            return phoneNumberMasked;
        }

        public void setPhoneNumberMasked(String phoneNumberMasked) {
            this.phoneNumberMasked = phoneNumberMasked;
        }

        public String getClientAction() {
            return clientAction;
        }

        public void setClientAction(String clientAction) {
            this.clientAction = clientAction;
        }

        public String getCancellationReason() {
            return cancellationReason;
        }

        public void setCancellationReason(String cancellationReason) {
            this.cancellationReason = cancellationReason;
        }

        public LocalDateTime getValidationTimestamp() {
            return validationTimestamp;
        }

        public void setValidationTimestamp(LocalDateTime validationTimestamp) {
            this.validationTimestamp = validationTimestamp;
        }

        public String getIdCarte() {
            return idCarte;
        }

        public void setIdCarte(String idCarte) {
            this.idCarte = idCarte;
        }
    }

    public static class CardRechargeHistoryDTO {
        private String requestId;
        private String idCarte;
        private BigDecimal montant;
        private String status;
        private String clientAction;
        private LocalDateTime timestamp;
        private LocalDateTime validationTimestamp;
        private String cancellationReason;
        private String numeroOrangeMoneyMasked;

        // Getters et setters
        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getIdCarte() {
            return idCarte;
        }

        public void setIdCarte(String idCarte) {
            this.idCarte = idCarte;
        }

        public BigDecimal getMontant() {
            return montant;
        }

        public void setMontant(BigDecimal montant) {
            this.montant = montant;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getClientAction() {
            return clientAction;
        }

        public void setClientAction(String clientAction) {
            this.clientAction = clientAction;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public LocalDateTime getValidationTimestamp() {
            return validationTimestamp;
        }

        public void setValidationTimestamp(LocalDateTime validationTimestamp) {
            this.validationTimestamp = validationTimestamp;
        }

        public String getCancellationReason() {
            return cancellationReason;
        }

        public void setCancellationReason(String cancellationReason) {
            this.cancellationReason = cancellationReason;
        }

        public String getNumeroOrangeMoneyMasked() {
            return numeroOrangeMoneyMasked;
        }

        public void setNumeroOrangeMoneyMasked(String numeroOrangeMoneyMasked) {
            this.numeroOrangeMoneyMasked = numeroOrangeMoneyMasked;
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        log.info("🔧 Test endpoint appelé");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "moneyservice");
        response.put("message", "Service moneyservice fonctionnel");
        response.put("port", 8095);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheckAgence() {
        Map<String, Object> health = new HashMap<>();

        try {

            health.put("status", "UP");
            health.put("service", "moneyservice");
            health.put("version", "2.0.0");
            health.put("port", 8095);
            health.put("timestamp", LocalDateTime.now());
            health.put("database", "CONNECTED");

        } catch (Exception e) {
            log.error("❌ Erreur health check agence: {}", e.getMessage());
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("database", "DISCONNECTED");
        }

        return ResponseEntity.ok(health);
    }

}