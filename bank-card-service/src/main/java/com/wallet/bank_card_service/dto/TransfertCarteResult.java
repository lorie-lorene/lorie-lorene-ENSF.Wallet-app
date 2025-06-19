package com.wallet.bank_card_service.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransfertCarteResult {
    private boolean success;
    private String transactionId;
    private BigDecimal montantTransfere;
    private BigDecimal nouveauSoldeCompte;
    private BigDecimal nouveauSoldeCarte;
    private BigDecimal frais;
    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
    
    public static TransfertCarteResult success(String transactionId, BigDecimal montant, 
            BigDecimal nouveauSoldeCompte, BigDecimal nouveauSoldeCarte, BigDecimal frais) {
        TransfertCarteResult result = new TransfertCarteResult();
        result.success = true;
        result.transactionId = transactionId;
        result.montantTransfere = montant;
        result.nouveauSoldeCompte = nouveauSoldeCompte;
        result.nouveauSoldeCarte = nouveauSoldeCarte;
        result.frais = frais;
        result.message = "Transfert effectué avec succès";
        result.timestamp = LocalDateTime.now();
        return result;
    }
    
    public static TransfertCarteResult failed(String errorCode, String message) {
        TransfertCarteResult result = new TransfertCarteResult();
        result.success = false;
        result.errorCode = errorCode;
        result.message = message;
        result.timestamp = LocalDateTime.now();
        return result;
    }
}

