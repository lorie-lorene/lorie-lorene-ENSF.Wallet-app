package com.wallet.money.controller;

import com.wallet.money.entity.*;
import com.wallet.money.service.RetraitMoneyService;
import com.wallet.money.service.TransactionService;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/withdrawals")
@RequiredArgsConstructor
public class RetraitController {
    
    @Autowired
    private RetraitMoneyService withdrawalService;
    
    @Autowired
    private TransactionService transactionService;

    @PostMapping()
    public ResponseEntity<PaymentResponse> withdraw(
            @RequestBody RetraitRequest request,
            @RequestHeader(value = "X-Client-ID", required = false) String clientId) {
        
        try {
            // 1. Créer transaction retrait
            Transaction transaction = createWithdrawalTransaction(clientId, request);
            
            // 2. Utiliser notre externalId
            request.setExternalId(transaction.getExternalId());
            
            // 3. Appeler FreemoPay
            PaymentResponse response = withdrawalService.initiateWithdrawal(request);
            
            // 4. Sauvegarder référence
            transactionService.updateFreemoReference(transaction.getId(), response.getReference());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private Transaction createWithdrawalTransaction(String clientId, RetraitRequest request) {
        Transaction transaction = new Transaction();
        transaction.setClientId(clientId != null ? clientId : "unknown");
        transaction.setExternalId("WIT_" + clientId + "_" + System.currentTimeMillis());
        transaction.setPhoneNumber(request.getReceiver());
        transaction.setAmount(BigDecimal.valueOf(request.getAmount()));
        transaction.setType("WITHDRAWAL");
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        
        return transactionService.transactionRepository.save(transaction);
    }
}
