package com.wallet.bank_card_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CarteCreationResult {
    private boolean success;
    private String idCarte;
    private String numeroCarte;
    private String errorCode;
    private String message;
    private BigDecimal fraisDebites;
    private LocalDateTime timestamp;
    
    public static CarteCreationResult success(String idCarte, String numeroCarte, BigDecimal frais) {
        CarteCreationResult result = new CarteCreationResult();
        result.success = true;
        result.idCarte = idCarte;
        result.numeroCarte = numeroCarte;
        result.fraisDebites = frais;
        result.message = "Carte créée avec succès";
        result.timestamp = LocalDateTime.now();
        return result;
    }
    
    public static CarteCreationResult failed(String errorCode, String message) {
        CarteCreationResult result = new CarteCreationResult();
        result.success = false;
        result.errorCode = errorCode;
        result.message = message;
        result.timestamp = LocalDateTime.now();
        return result;
    }
}
