package com.wallet.money.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.money.entity.PaymentRequest;
import com.wallet.money.entity.PaymentResponse;
import com.wallet.money.entity.Transaction;
import com.wallet.money.service.DepotMoneyService;
import com.wallet.money.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/deposit")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    @Autowired
    private DepotMoneyService freemoApiClient;

    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<PaymentResponse> makePayment(
            @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-Client-ID", required = false) String clientId) {

        log.info("[API] Dépôt demandé - Client: {}, Montant: {}", clientId, request.getAmount());

        try {
            // 1. Sauvegarder transaction en PENDING (simple)
            Transaction transaction = transactionService.createPendingDeposit(
                    clientId != null ? clientId : "unknown",
                    request.getPayer(),
                    request.getAmount());

            // 2. Utiliser notre externalId pour le suivi
            request.setExternalId(transaction.getExternalId());

            // 3. Appeler FreemoPay
            PaymentResponse response = freemoApiClient.createPayment2(request);

            // 4. Sauvegarder la référence FreemoPay
            transactionService.updateFreemoReference(transaction.getId(), response.getReference());

            log.info("✅ Dépôt initié - ExternalId: {}, FreemoRef: {}",
                    transaction.getExternalId(), response.getReference());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors du dépôt: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{reference}")
    public ResponseEntity<PaymentResponse> checkPayment(@PathVariable String reference) {
        log.info("[API] Vérification du statut du paiement pour {}", reference);
        PaymentResponse response = freemoApiClient.getPaymentStatus(reference);
        return ResponseEntity.ok(response);
    }

    
}