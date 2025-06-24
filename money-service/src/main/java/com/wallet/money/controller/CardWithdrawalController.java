package com.wallet.money.controller;

import com.wallet.money.entity.*;
import com.wallet.money.service.RetraitMoneyService;
import com.wallet.money.service.TransactionService;
import com.wallet.money.service.CardServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * NOUVEAU ENDPOINT: Retrait depuis carte vers Mobile Money
     */
    @PostMapping("/from-card")
    public ResponseEntity<PaymentResponse> withdrawFromCard(
            @RequestBody CardWithdrawalRequest request,
            @RequestHeader(value = "X-Client-ID", required = false) String clientId,
            @RequestHeader(value = "X-Source-Service", required = false) String sourceService) {

        try {
            log.info("💸 [CARD-WITHDRAWAL] Demande reçue - Carte: {}, Montant: {}, Provider: {}",
                    request.getIdCarte(), request.getAmount(), request.getProvider());

            // 1. Créer transaction de type CARD_WITHDRAWAL
            Transaction transaction = createCardWithdrawalTransaction(clientId, request);

            // 2. Préparer requête pour RetraitMoneyService (utilise l'infrastructure
            // existante)
            RetraitRequest retraitRequest = new RetraitRequest();
            retraitRequest.setReceiver(request.getReceiver());
            retraitRequest.setAmount(request.getAmount());
            retraitRequest.setDescription(request.getDescription());
            retraitRequest.setExternalId(transaction.getExternalId());
            retraitRequest.setCallback("http://localhost:8095/webhook/freemopay/card-withdrawal");

            // 3. Utiliser le service de retrait existant
            PaymentResponse response = withdrawalService.initiateWithdrawal(retraitRequest);

            // 4. Sauvegarder référence FreemoPay
            transactionService.updateFreemoReference(transaction.getId(), response.getReference());

            // 5. Adapter la réponse
            response.setMessage("Retrait initié depuis carte " + request.getIdCarte() +
                    ". " + response.getMessage());

            log.info("✅ [CARD-WITHDRAWAL] Retrait initié - ExternalId: {}, FreemoRef: {}",
                    transaction.getExternalId(), response.getReference());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [CARD-WITHDRAWAL] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Vérifier le statut d'un retrait carte
     */
    @GetMapping("/card-withdrawal/status/{requestId}")
    public ResponseEntity<PaymentResponse> getCardWithdrawalStatus(@PathVariable String requestId) {
        Transaction transaction = transactionService.findByExternalId(requestId);

        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        PaymentResponse response = new PaymentResponse();
        response.setReference(transaction.getFreemoReference());
        response.setStatus(transaction.getStatus());
        response.setMessage("Statut retrait carte: " + transaction.getStatus());

        return ResponseEntity.ok(response);
    }

    private Transaction createCardWithdrawalTransaction(String clientId, CardWithdrawalRequest request) {
        Transaction transaction = new Transaction();
        transaction.setClientId(clientId != null ? clientId : "unknown");
        transaction.setExternalId("CARD_WITHDRAWAL_" + request.getIdCarte() + "_" + System.currentTimeMillis());
        transaction.setPhoneNumber(request.getReceiver());
        transaction.setAmount(BigDecimal.valueOf(request.getAmount()));
        transaction.setType("CARD_WITHDRAWAL"); // Nouveau type
        transaction.setStatus("PENDING");
        transaction.setIdCarte(request.getIdCarte());
        transaction.setCallbackUrl(request.getCallbackUrl());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        return transactionService.transactionRepository.save(transaction);
    }
}
