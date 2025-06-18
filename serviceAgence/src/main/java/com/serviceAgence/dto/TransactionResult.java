package com.serviceAgence.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResult {
    private boolean success;
    private String transactionId;
    private String errorCode;
    private String message;
    private BigDecimal montant;
    private BigDecimal frais;
    private LocalDateTime timestamp;

    public static TransactionResult success(String transactionId, BigDecimal montant, BigDecimal frais) {
        TransactionResult result = new TransactionResult();
        result.setSuccess(true);
        result.setTransactionId(transactionId);
        result.setMontant(montant);
        result.setFrais(frais);
        result.setMessage("Transaction réussie");
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

    public static TransactionResult failed(String errorCode, String message) {
        TransactionResult result = new TransactionResult();
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
}
