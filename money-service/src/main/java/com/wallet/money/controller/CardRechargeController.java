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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
            if (request.getMontant().compareTo(new BigDecimal("500")) < 0) {
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
                    "http://localhost:8096/webhook/freemopay");

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
        transaction.setExternalId("CARD_RECHARGE_" + request.getIdCarte() + "_" + System.currentTimeMillis());
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

    /**
     * Historique des transactions d'un client
     */
    // @GetMapping("/my-history")
    // @PreAuthorize("hasRole('CLIENT')")
    // public ResponseEntity<Page<TransactionDTO>> getMyTransactionHistory(
    // @RequestParam(defaultValue = "0") int page,
    // @RequestParam(defaultValue = "20") int size,
    // @RequestParam(required = false) String type,
    // @RequestParam(required = false) String status,
    // @RequestParam(required = false) @DateTimeFormat(iso =
    // DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
    // @RequestParam(required = false) @DateTimeFormat(iso =
    // DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
    // Authentication authentication) {

    // try {
    // String clientId = extractClientId(authentication);

    // log.info("📊 Récupération historique transactions - Client: {}, Page: {}/{}",
    // clientId, page, size);

    // PageRequest pageRequest = PageRequest.of(page, size,
    // Sort.by(Sort.Direction.DESC, "createdAt"));

    // Page<Transaction> transactions = transactionService.findClientTransactions(
    // clientId, type, status, startDate, endDate, pageRequest);

    // // Convertir en DTO pour masquer les infos sensibles
    // Page<TransactionDTO> result = transactions.map(this::convertToDTO);

    // return ResponseEntity.ok(result);

    // } catch (Exception e) {
    // log.error("❌ Erreur récupération historique: {}", e.getMessage(), e);
    // return ResponseEntity.badRequest().build();
    // }
    // }

    // /**
    // * Détails d'une transaction spécifique
    // */
    // @GetMapping("/{transactionId}")
    // @PreAuthorize("hasRole('CLIENT')")
    // public ResponseEntity<TransactionDTO> getTransactionDetails(
    // @PathVariable String transactionId,
    // Authentication authentication) {

    // try {
    // String clientId = extractClientId(authentication);

    // Transaction transaction = transactionService.findByExternalIdAndClientId(
    // transactionId, clientId);

    // if (transaction == null) {
    // return ResponseEntity.notFound().build();
    // }

    // TransactionDTO dto = convertToDTO(transaction);
    // return ResponseEntity.ok(dto);

    // } catch (Exception e) {
    // log.error("❌ Erreur récupération transaction: {}", e.getMessage(), e);
    // return ResponseEntity.badRequest().build();
    // }
    // }

    // /**
    // * Statistiques des transactions du client
    // */
    // @GetMapping("/statistics")
    // @PreAuthorize("hasRole('CLIENT')")
    // public ResponseEntity<Map<String, Object>> getTransactionStatistics(
    // @RequestParam(defaultValue = "30") int days,
    // Authentication authentication) {

    // try {
    // String clientId = extractClientId(authentication);

    // LocalDateTime startDate = LocalDateTime.now().minusDays(days);
    // Map<String, Object> stats = transactionService.getClientStatistics(
    // clientId, startDate);

    // return ResponseEntity.ok(stats);

    // } catch (Exception e) {
    // log.error("❌ Erreur récupération statistiques: {}", e.getMessage(), e);
    // return ResponseEntity.badRequest().build();
    // }
    // }

    // /**
    // * Historique spécifique des recharges de cartes
    // */
    // @GetMapping("/card-recharges")
    // @PreAuthorize("hasRole('CLIENT')")
    // public ResponseEntity<List<CardRechargeHistoryDTO>> getCardRechargeHistory(
    // @RequestParam(required = false) String idCarte,
    // @RequestParam(defaultValue = "50") int limit,
    // Authentication authentication) {

    // try {
    // String clientId = extractClientId(authentication);

    // List<Transaction> recharges = transactionService.findCardRecharges(
    // clientId, idCarte, limit);

    // List<CardRechargeHistoryDTO> result = recharges.stream()
    // .map(this::convertToCardRechargeDTO)
    // .toList();

    // return ResponseEntity.ok(result);

    // } catch (Exception e) {
    // log.error("❌ Erreur récupération historique recharges: {}", e.getMessage(),
    // e);
    // return ResponseEntity.badRequest().build();
    // }
    // }

    // /**
    // * Recherche de transactions
    // */
    // @GetMapping("/search")
    // @PreAuthorize("hasRole('CLIENT')")
    // public ResponseEntity<List<TransactionDTO>> searchTransactions(
    // @RequestParam String query,
    // @RequestParam(defaultValue = "20") int limit,
    // Authentication authentication) {

    // try {
    // String clientId = extractClientId(authentication);

    // List<Transaction> transactions = transactionService.searchClientTransactions(
    // clientId, query, limit);

    // List<TransactionDTO> result = transactions.stream()
    // .map(this::convertToDTO)
    // .toList();

    // return ResponseEntity.ok(result);

    // } catch (Exception e) {
    // log.error("❌ Erreur recherche transactions: {}", e.getMessage(), e);
    // return ResponseEntity.badRequest().build();
    // }
    // }
    // private String extractClientId(Authentication authentication) {
    // if (authentication.getPrincipal() instanceof
    // org.springframework.security.core.userdetails.UserDetails) {
    // org.springframework.security.core.userdetails.UserDetails userDetails =
    // (org.springframework.security.core.userdetails.UserDetails)
    // authentication.getPrincipal();
    // return userDetails.getUsername();
    // }
    // throw new SecurityException("Client non authentifié");
    // }

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

}