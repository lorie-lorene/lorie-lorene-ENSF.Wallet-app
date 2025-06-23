package com.m1_fonda.serviceUser.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 💰 Transaction Response DTO
 * Response for financial operations (deposit, withdrawal, transfer)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String transactionId;
    private String status; // SUCCESS, FAILED, PENDING, REJECTED
    private String message;
    private BigDecimal montant;
    private BigDecimal frais;
    private BigDecimal montantNet;
    
    private String numeroCompteSource;
    private String numeroCompteDestination;
    private String typeOperation;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;
    
    private String referenceNumber;
    private String description;
    
    // Error details if transaction failed
    private String errorCode;
    private String errorDetails;
    
    // Constructor for backward compatibility
    public TransactionResponse(String transactionId, String status, String message, 
                             BigDecimal montant, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
        this.montant = montant;
        this.timestamp = timestamp;
    }
}