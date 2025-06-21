package com.wallet.money.controller;

import com.wallet.money.carteclient.CardRechargeRequest;
import com.wallet.money.carteclient.CardRechargeResponse;
import com.wallet.money.entity.PaymentRequest;
import com.wallet.money.entity.PaymentResponse;
import com.wallet.money.entity.Transaction;
import com.wallet.money.service.DepotMoneyService;
import com.wallet.money.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
                    "http://localhost:8084/webhook/freemopay");

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

}